$(function () {
    $("#topButton").click(setTop);
    $("#wonderfulButton").click(setWonderful);
    $("#deleteButton").click(setDelete);
});

function setTop() {
    $.post(
        CONTEXT_PATH + "/post/setTop",
        {"postId": $("#thePostId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#topButton").attr("disabled", "disabled");
            } else {
                alert(data.msg)
            }
        }
    );
}

function setWonderful() {
    $.post(
        CONTEXT_PATH + "/post/setWonderful",
        {"postId": $("#thePostId").val()},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                $("#wonderfulButton").attr("disabled", "disabled");
            } else {
                alert(data.msg)
            }
        }
    );
}

function setDelete() {
    if (confirm("确定删除帖子？")) {
        $.post(
            CONTEXT_PATH + "/post/delete",
            {"postId": $("#thePostId").val()},
            function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                    location.href = CONTEXT_PATH + "/index";
                } else {
                    alert(data.msg)
                }
            }
        );
    }
}

function like(btn, entityType, entityId, targetUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType": entityType, "entityId": entityId, "targetUserId": targetUserId, "postId": postId},
        function (data) {
            data = $.parseJSON(data);
            if (data.code == 0) {
                //因为this=“#xxx”，所以直接$(xxx),使用children选择孩子中的b标签
                $(btn).children("b").text(data.likeStatus == 1 ? "已赞" : "赞");
                $(btn).children("i").text(data.likeCount);
            } else {
                alert(data.msg);
            }
        }
    );
}

