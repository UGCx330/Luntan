//给获取验证码按钮绑定单击函数
$(function () {
    $("#verifyCodeBtn").click(sendKaptchaToEmail);
});

//ajax异步请求
function sendKaptchaToEmail() {
    var email = $("#your-email").val();
    if (!email) {
        alert("请输入邮箱");
        return false;
    }
    $.get(
        CONTEXT_PATH+"/sendKaptchaToEmail",//url
        {"email":email},//携带参数
        function (data) {//服务器返回Json处理
            data=$.parseJSON(data);//Json字符串解析为Json对象
            if (data.code==0){
                alert("验证码已发送至您的邮箱,请登录邮箱查看!");
            }else {
                alert(data.msg);
            }
        }
    );
}