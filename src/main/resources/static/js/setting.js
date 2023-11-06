//给更新头像按钮绑定异步请求,切记upload不能加（），否则就会一直执行此方法，我们是要绑定
$(function () {
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax(
        {
            url: "http://upload-z1.qiniup.com",
            method: "post",
            processData: false,
            contentType: false,
            //此处data为传给七牛云的数据,new FormData为表单对象，且要求是js对象，然而我们使用id选择器生成的是jquery对象
            //jquery对象的本质是dom数组，所以取0位置的即为js对象。
            data: new FormData($("#uploadForm")[0]),
            success: function (data) {
                if (data && data.code == 0) {
                    //上传成功后，再次访问论坛服务器，将用户表中的头像更新为七牛云路径
                    $.post(
                        CONTEXT_PATH + "/user/updateHeader",
                        //input为元素选择器，[]为name选择器
                        {"fileName": $("input[name='key']").val()},
                        function (data) {
                            data = $.parseJSON(data);
                            if (data.code == 0) {
                                window.location.reload();
                            } else {
                                alert(data.msg);
                            }
                        }
                    );
                } else {
                    alert("上传失败");
                }
            }
        }
    );
    //执行完异步请求后，停止，防止submit继续提交
    return false;
}
