<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="action" type="java.lang.String"--%>
<%--@elvariable id="gameId" type="long"--%>
<%--@elvariable id="username" type="java.lang.String"--%>
<!DOCTYPE html>
<html>
<head>
    <title>三连棋游戏</title>
    <link href="//cdn.bootcss.com/bootstrap/2.3.1/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<c:url value="/resource/stylesheet/chessGame.css"/>">
    <script src="//cdn.bootcss.com/jquery/2.2.2/jquery.min.js"></script>
    <script src="//cdn.bootcss.com/bootstrap/2.3.1/js/bootstrap.min.js"></script>
</head>
<body>
<h2>三连棋游戏</h2>
<span class="player-label">您：${username}</span>
<span class="player-label">对手：</span>
<span id="opponent">等待中</span>
<div id="status">&nbsp;</div>
<div id="gameContainer">
    <div class="row">
        <div id="r0c0" class="game-cell" onclick="move(0,0);">&nbsp;</div>
        <div id="r0c1" class="game-cell" onclick="move(0,1);">&nbsp;</div>
        <div id="r0c2" class="game-cell" onclick="move(0,2);">&nbsp;</div>
    </div>
    <div class="row">
        <div id="r1c0" class="game-cell" onclick="move(1,0);">&nbsp;</div>
        <div id="r1c1" class="game-cell" onclick="move(1,1);">&nbsp;</div>
        <div id="r1c2" class="game-cell" onclick="move(1,2);">&nbsp;</div>
    </div>
    <div class="row">
        <div id="r2c0" class="game-cell" onclick="move(2,0);">&nbsp;</div>
        <div id="r2c1" class="game-cell" onclick="move(2,1);">&nbsp;</div>
        <div id="r2c2" class="game-cell" onclick="move(2,2);">&nbsp;</div>
    </div>
</div>
<div id="modalWaiting" class="modal hide fade">
    <div class="modal-header"><h3>请等待...</h3></div>
    <div class="modal-body" id="modalWaitingBody">&nbsp;</div>
</div>
<div id="modalError" class="modal hide fade">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3>错误</h3>
    </div>
    <div class="modal-body" id="modalErrorBody">发生了错误</div>
    <div class="modal-footer">
        <button class="btn btn-primary" data-dismiss="modal">好的</button>
    </div>
</div>
<div id="modalGameOver" class="modal hide fade">
    <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h3>游戏结束</h3>
    </div>
    <div class="modal-body" id="modalGameOverBody">&nbsp;</div>
    <div class="modal-footer">
        <button class="btn btn-primary" data-dismiss="modal">好的</button>
    </div>
</div>
<script type="text/javascript">
    var move;
    $(document).ready(function () {
        var modalError = $("#modalError");
        var modalErrorBody = $("#modalErrorBody");
        var modalWaiting = $("#modalWaiting");
        var modalWaitingBody = $("#modalWaitingBody");
        var modalGameOver = $("#modalGameOver");
        var modalGameOverBody = $("#modalGameOverBody");
        var opponent = $("#opponent");
        var status = $("#status");
        var opponentUsername;
        var username = "<c:out value="${username}"/>";
        var myTurn = false;

        $('.game-cell').addClass("span1");

        if(!("WebSocket" in window)) {
            modalErrorBody.text("当前浏览器不支持WebSocket，请使用IE10或者最新版本的火狐或者谷歌浏览器")
            modalError.modal("show");
            return;
        }

        modalWaitingBody.text("正在连接服务器.")
        modalWaiting.modal({keyboard: false, show: true});

        var server;
        try{
            server = new WebSocket('ws://' + window.location.host + '<c:url value="/chessGame/${gameId}/${username}"><c:param name="action" value="${action}" /></c:url> ');
        }catch (error) {
            modalWaiting.modal('hide');
            modalErrorBody.text(error);
            modalError.modal("show");
            return;
        }

        server.onopen = function (event) {
            modalWaitingBody.text('等待你的对手加入游戏.')
            modalWaiting.modal({kyeboard: false, show: true});
        }

        window.onbeforeunload = function () {
            server.close();
        }

        server.onclose = function (event) {
            if(!event.wasClean || event.code != 1000) {
                toggleTurn(false, '游戏结束,由于错误!');
                modalWaiting.modal('hide');
                modalErrorBody.text('Code' + event.code + ": " + event.reason);
                modalError.modal('show');
            }
        }

        server.onmessage = function (event) {
            var message = JSON.parse(event.data);
            if(message.action == 'gameStarted') {
                if(message.game.player1 == username)
                    opponentUsername = message.game.player2;
                else
                    opponentUsername = message.game.player1;
                opponent.text(opponentUsername);
                toggleTurn(message.game.nextMoveBy == username);
                modalWaiting.modal('hide');
            }else if(message.action == 'opponentMadeMove') {
                $('#r' + message.move.row + 'c' + message.move.column).unbind('click').removeClass('game-cell-selectable').addClass('game-cell-opponent game-cell-taken');
                toggleTurn(true);
            } else if(message.action = 'gameOver') {
                toggleTurn(false, '游戏结束!');
                if(message.winner) {
                    modalGameOverBody.text("恭喜您赢了!");
                }else {
                    modalGameOverBody.text('用户 "' + opponentUsername + '" 赢了.');
                    modalGameOver.modal('show');
                }
            }else if(message.action == 'gameIsDraw') {
                toggleTurn(false, '平局，没有赢家.');
                modalGameOverBody.text('游戏平局，没有人胜利.');
                modalGameOver.modal('show');
            }else if(message.action == 'gameForfeited') {
                toggleTurn(false, 'Your opponent forfeited!');
                modalGameOverBody.text('用户 "' + '" 弃权， 您赢了');
                modalGameOver.modal('show');
            }
        }

        var toggleTurn = function (isMyTurn, message) {
            myTurn = isMyTurn;
            if(myTurn) {
                status.text(message || '该您了!');
                $('.game-cell:not(.game-cell-taken)').addClass('game-cell-selectable');
            } else {
                status.text(message || '等待您的对手操作.')
                $('.game-cell-selectable').removeClass('game-cell-selectable');
            }
        };

        var move = function (row, column) {
            if(!myTurn) {
                modalErrorBody.text('还不是你的回合哦!');
                modalError.modal('show');
                return;
            }
            if(server != null) {
                server.send(JSON.stringify({row: row, column: column}));
                $('#r' + row + 'c' + column).unbind('click').removeClass('game-cell-selectable').addClass('game-cell-player game-cell-taken');
                toggleTurn(false);
            } else {
                modalErrorBody.text('没有连接到服务器.');
                modalError.modal('show');
            }
        }
    })
</script>
</body>
</html>














