package com.chaokunyang.chess;

import org.apache.commons.lang3.math.NumberUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author chaokunyang
 * @create 2017/2/26
 */
@WebServlet(
        name = "chessGame",
        urlPatterns = "/chessGame"
)
public class ChessServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("pendingGames", ChessGame.getPendingGames());
        this.view("list", req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if("join".equalsIgnoreCase(action)) {
            String gameIdString = req.getParameter("gameId");
            String username = req.getParameter("username");
            if(username == null || gameIdString == null || !NumberUtils.isDigits(gameIdString))
                this.list(req, resp);
            else {
                req.setAttribute("action", "join");
                req.setAttribute("username", username);
                req.setAttribute("gameId", Long.parseLong(gameIdString));
                this.view("game", req, resp);
            }
        }else if("start".equalsIgnoreCase(action)) {
            String username = req.getParameter("username");
            if(username == null)
                this.list(req, resp);
            else {
                req.setAttribute("action", "start");
                req.setAttribute("username", username);
                req.setAttribute("gameId", ChessGame.queueGame(username));
                this.view("game", req, resp);
            }
        }else
            this.list(req, resp);
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + "/chessGame"));
    }

    private void view(String view, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("//WEB-INF/jsp/view/chessGame/" + view + ".jsp").forward(req, resp);
    }

}
