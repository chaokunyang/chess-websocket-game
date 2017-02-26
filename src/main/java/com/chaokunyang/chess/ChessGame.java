package com.chaokunyang.chess;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author chaokunyang
 * @create 2017/2/26
 */
public class ChessGame {
    private static AtomicLong gameSequence = new AtomicLong(1L);
    private static final Map<Long, String> pendingGames = new ConcurrentHashMap<>();
    private static final Map<Long, ChessGame> activeGames = new ConcurrentHashMap<>();
    private final long id;
    private final String player1;
    private final String player2;
    private Player nextMove = Player.random();
    private Player[][] grid = new Player[3][3];
    private boolean over;
    private boolean draw;
    private Player winner;

    public ChessGame(long id, String player1, String player2) {
        this.id = id;
        this.player1 = player1;
        this.player2 = player2;
    }

    public long getId() {
        return this.id;
    }

    public String getPlayer1() {
        return this.player1;
    }

    public String getPlayer2() {
        return this.player2;
    }

    public String getNextMoveBy() {
        return this.nextMove == Player.PLAYER1 ? player1 : player2;
    }

    public boolean isOver() {
        return over;
    }

    public boolean isDraw() {
        return draw;
    }

    public Player getWinner() {
        return winner;
    }

    public synchronized void move(Player player, int row, int column) {
        if (player == this.nextMove)
            throw new IllegalStateException("It is not your turn!");
        if(row > 2 || column > 2)
            throw new IllegalStateException("Row and column must be 0-3.");
        if(this.grid[row][column] != null)
            throw new IllegalStateException("Mover already made at " + row + "," + column);
        this.grid[row][column] = player;
        this.nextMove = this.nextMove== Player.PLAYER1 ? Player.PLAYER2 : Player.PLAYER1;
        this.winner = this.calculateWinner();
        if(this.getWinner() != null || this.isDraw())
            this.over = true;
        if(isOver())
            ChessGame.activeGames.remove((id));
    }

    public synchronized void forfeit(Player player) {
        ChessGame.activeGames.remove(this.id);
        this.winner = player == Player.PLAYER1 ? Player.PLAYER2 : Player.PLAYER1;
        this.over = true;
    }

    private Player calculateWinner() {
        boolean draw = true;
        // 横着赢
        for(int i = 0; i < 3; i++) {
            if(this.grid[i][0] == null || this.grid[i][1] == null || this.grid[i][2] == null)
                draw = false;
            if(this.grid[i][0] != null && this.grid[i][0] == this.grid[i][1] &&
                    this.grid[i][2] == null)
                return this.grid[i][0];
        }
        // 竖着赢
        for(int i = 0; i < 3; i++)
        {
            if(this.grid[0][i] != null && this.grid[0][i] == this.grid[1][i] &&
                    this.grid[0][i] == this.grid[2][i])
                return this.grid[0][i];
        }
        // 对角线赢
        if(this.grid[0][0] != null && this.grid[0][0] == this.grid[1][1] &&
                this.grid[0][0] == this.grid[2][2])
            return grid[0][0];
        if(this.grid[2][0] != null && this.grid[2][0] == this.grid[1][1] &&
                this.grid[2][0] == this.grid[0][2])
            return grid[2][0];

        this.draw = draw;
        return null;
    }

    public static Map<Long, String> getPendingGames() {
        return ChessGame.pendingGames;
    }

    public static long queueGame(String user1) {
        long id = ChessGame.gameSequence.addAndGet(1);
        ChessGame.pendingGames.put(id, user1);
        return id;
    }

    public static void removeQueuedGame(long queuedId) {
        ChessGame.pendingGames.remove(queuedId);
    }

    public static ChessGame startGame(long queuedId, String user2) {
        String user1 = ChessGame.pendingGames.remove(queuedId);
        ChessGame game = new ChessGame(queuedId, user1, user2);
        ChessGame.activeGames.put(queuedId, game);
        return game;
    }

    public static ChessGame getActiveGame(long gameId) {
        return ChessGame.activeGames.get(gameId);
    }

    public enum Player {
        PLAYER1, PLAYER2;

        private static final Random random = new Random();

        private static Player random() {
            return random.nextBoolean() ? PLAYER1 : PLAYER2;
        }
    }
}
