<%--@elvariable id="pendingGames" type="java.util.Map<long, java.Lang.String>"--%>
<!DOCTYPE html>
<html>
    <head>
        <title>三连棋游戏</title>
        <script src="//cdn.bootcss.com/jquery/2.2.2/jquery.min.js"></script>
    </head>
    <body>
        <h2>三连棋游戏</h2>
        <a href="javascript: void 0;" onclick="startGame();">开始游戏</a><br>
        <br>
        <c:choose>
            <c:when test="${fn:length(pendingGames) == 0}">
                <i>没有正在等待的游戏可以加入</i>
            </c:when>
            <c:otherwise>
                加入游戏等待另一个玩家:<br>
                <c:forEach items="${pendingGames}" var="e">
                    <a href="javascript:void 0;" onclick="joinGame(${e.key});">用户： ${e.value}</a><br>
                </c:forEach>
            </c:otherwise>
        </c:choose>

        <script type="text/javascript">
            var startGame, joinGame;
            $(document).ready(function () {
                var url = '<c:url value="/chessGame"/>';
                startGame = function () {
                    var username = prompt('输入用户名', '');
                    if(username != null && username.trim().length > 0 && validateUsername(username))
                        post({action: 'start', username: username});
                }

                joinGame = function (gameId) {
                    var username = prompt("输入用户名", '');
                    if(username != null && username.trim().length > 0 && validateUsername(username))
                        post({action: 'join', username: username, gameId: gameId});
                };

                var validateUsername = function (username) {
                    var valid = username.match(/^[a-zA-Z0-9_]+$/) != null;
                    if(!valid)
                        alert('用户名只可以包含字母、数字、下划线.');
                    return valid;
                }

                var post = function (fields) {
                    var form = $('<form id="mapForm" method="post"></form>').attr({action: url, style: 'display: none;'});
                    for(var key in fields) {
                        if(fields.hasOwnProperty(key))
                            form.append($('<input type="hidden">').attr({
                                name: key, value: fields[key]
                            }));
                    }
                    $('body').append(form);
                    form.submit();
                }
            })
        </script>
    </body>
</html>
