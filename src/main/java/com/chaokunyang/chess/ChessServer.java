package com.chaokunyang.chess;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chaokunyang
 * @create 2017/2/26
 */
@ServerEndpoint("/chessGame/{gameId}/{username}")
public class ChessServer {
    private static Map<Long, Game> games = new ConcurrentHashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();

    @OnOpen
    public void onOpen(Session session, @PathParam("gameId") long gameId, @PathParam("username") String username) {
        try {
            ChessGame chessGame = ChessGame.getActiveGame(gameId);
            if (chessGame != null) {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "当前游戏已经开始了"));

                List<String> actions = session.getRequestParameterMap().get("action");
                if (actions != null && actions.size() == 1) {
                    String action = actions.get(0);
                    if ("start".equalsIgnoreCase(action)) {
                        Game game = new Game();
                        game.gameId = gameId;
                        game.player1 = session;
                        games.put(gameId, game);
                    } else if ("join".equalsIgnoreCase(action)) {
                        Game game = games.get(gameId);
                        game.player2 = session;
                        game.chessGame = ChessGame.startGame(gameId, username);
                        this.sendJsonMessage(game.player1, game, new GameStartedMessage(game.chessGame));
                        this.sendJsonMessage(game.player2, game, new GameStartedMessage(game.chessGame));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.toString()));
            } catch (IOException e1) {
            }
        }
    }

    @OnMessage
    public void onMessage(Session session, String message, @PathParam("gameId") long gameId) {
        Game game = games.get(gameId);
        boolean isPlayer1 = session == game.player1;

        try {
            Move move = mapper.readValue(message, Move.class);
            game.chessGame.move(isPlayer1 ? ChessGame.Player.PLAYER1 : ChessGame.Player.PLAYER2, move.getRow(), move.getColumn());
            sendJsonMessage((isPlayer1 ? game.player2 : game.player1), game, new OpponentMadeMoveMessage(move));
            if(game.chessGame.isOver()) {
                if(game.chessGame.isDraw()) {
                    sendJsonMessage(game.player1, game, new GameIsDrawMessage());
                    sendJsonMessage(game.player2, game, new GameIsDrawMessage());
                }else {
                    boolean wasPlayer1 = game.chessGame.getWinner() == ChessGame.Player.PLAYER1;
                    this.sendJsonMessage(game.player1, game, new GameOverMessage(wasPlayer1));
                    this.sendJsonMessage(game.player2, game, new GameOverMessage(!wasPlayer1));
                }
                game.player1.close();
                game.player2.close();
            }
        } catch (IOException e) {
            this.handleException(e, game);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("gameId") long gameId) {
        Game game = games.get(gameId);
        if(game == null)
            return;
        boolean isPlayer1 = session == game.player1;
        if(game.chessGame == null) {
            ChessGame.removeQueuedGame(gameId);
        }else if(!game.chessGame.isOver()) {
            game.chessGame.forfeit(isPlayer1 ? ChessGame.Player.PLAYER1 : ChessGame.Player.PLAYER2);
            Session opponent = isPlayer1 ? game.player2 : game.player1;
            sendJsonMessage(opponent, game, new GameForfeitedMessage());
            try {
                opponent.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendJsonMessage(Session session, Game game, Message message) {
        try {
            session.getBasicRemote().sendText(mapper.writeValueAsString(message));
        } catch (IOException e) {
            this.handleException(e, game);
        }
    }

    private void handleException(Throwable t, Game game) {
        t.printStackTrace();
        String message = t.toString();
        try {
            game.player1.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, message));
        } catch (IOException e) {
        }
        try {
            game.player2.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, message));
        } catch (IOException e) {
        }
    }


    public static class Game {
        public long gameId;

        public Session player1;

        public Session player2;

        public ChessGame chessGame;
    }

    public static class Move {
        private int row;
        private int column;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getColumn() {
            return column;
        }

        public void setColumn(int column) {
            this.column = column;
        }
    }

    public static abstract class Message {
        private final String action;

        public Message(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }
    }

    public static class GameStartedMessage extends Message {
        private final ChessGame game;

        public ChessGame getGame() {
            return game;
        }

        public GameStartedMessage(ChessGame game) {
            super("gameStarted");
            this.game = game;
        }
    }

    public static class OpponentMadeMoveMessage extends Message {
        private Move move;

        public OpponentMadeMoveMessage(Move move) {
            super("opponentMadeMove");
            this.move = move;
        }

        public Move getMove() {
            return move;
        }
    }

    public static class GameOverMessage extends Message {
        private final boolean winner;

        public GameOverMessage(boolean winner) {
            super("gameOver");
            this.winner = winner;
        }

        public boolean isWinner() {
            return winner;
        }
    }

    public static class GameIsDrawMessage extends Message {
        public GameIsDrawMessage() {
            super("gameIsDraw");
        }
    }

    public static class GameForfeitedMessage extends Message {
        public GameForfeitedMessage() {
            super("gameForfeited");
        }
    }
}