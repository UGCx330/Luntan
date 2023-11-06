$(function () {
    $("#publishBtn").click(publish);
});

function publish() {
    $("#publishModal").modal("hide");
    //最终给服务器的图片名字
    var fileNames = "";
    var tempName = "";
    var videoName = "";
    var formData;
    //循环上传图片
    for (var i = 0; i < imgArr.length; i++) {
        var file = baseToFile(imgArr[i], imgArrType[i]);
        formData = new FormData();
        formData.append('token', $("#token").val());
        tempName = $("#userId").val() + "_" + Math.round(new Date());
        formData.append('key', tempName);
        formData.append('file', file);
        fileNames += tempName + "q";
        //上传图片到七牛云
        $.ajax(
            {
                url: "http://upload-z1.qiniup.com",
                method: "post",
                processData: false,
                contentType: false,
                data: formData,
                success: function (data) {
                    if (data.code != 0) {
                        alert("上传图片超时，请稍后再试");
                        //一旦出错立刻终结上传
                        return;
                    }
                }
            }
        );
    }//上传图片循环到此结束

    //图片上传完毕开始上传video到七牛云
    var videoUrl = $("#userId").val() + "_video_" + Math.round(new Date());
    var video = $("#videoId").prop("files")[0];
    formData = new FormData();
    formData.append('token', $("#videoToken").val());
    formData.append('key', videoUrl);
    formData.append('file', video);
    $("#hintBody").text("正在上传");
    //显示提示框
    $("#hintModal").modal("show");
    $.ajax(
        {
            url: "http://upload-z1.qiniup.com",
            method: "post",
            processData: false,
            contentType: false,
            data: formData,
            success: function (data) {
                $("#hintModal").modal("hide");
                if (data.code != 0) {
                    alert("上传视频超时，请稍后再试");
                    return;
                }
                alert("上传成功!");
            }
        }
    );

    var iframe = $("#iframeId").val();

    //图片上传完毕后，将帖子存入数据库
    var title = $("#recipient-name").val();
    var content = $("#message-text").val();
    let likeArray = document.getElementsByName("plate");
    let like;
    let plate = '0';
    for (let i = 0; i < likeArray.length; i++) {
        if (likeArray[i].checked) {
            like = likeArray[i].value;
            plate += like;
        }
    }
    $.post(
        CONTEXT_PATH + "/post/add",
        {
            "title": title,
            "content": content,
            "fileNames": fileNames,
            "videoUrl": videoUrl,
            "iframe": iframe,
            "plate": plate
        },
        function (data) {
            data = $.parseJSON(data);
            //提示框中显示服务器的msg
            $("#hintBody").text(data.msg);
            //显示提示框
            $("#hintModal").modal("show");
            //2秒后关闭提示框,状态码为0成功发布后重新访问当前页面，否则不做反应
            setTimeout(function () {
                $("#hintModal").modal("hide");
                if (data.code == 0) {
                    window.location.reload();
                }
            }, 2000);
        }
    );
}


var count = 0;

var imgArr = [];//图片数组
var imgArrType = [];//图片类型数组


/**
 * 唤起选择文件控件
 */
function onclickFile() {
    document.getElementById('toFileId').click();
}


/**
 * 选择图片后 回调文件处理
 * @param files
 */
function onchangeFile(files) {
    if (count >= 3) {
        alert("存储紧张，暂时只支持上传三张图片~");
        return false;
    }
    var file = files.files[0];
    //获取文件后缀
    var fileType = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length);

    // //文件重命名，需要新new文件复制
    // file = new File([file], $("#userId").val() + "_" + Math.round(new Date()), {type: fileType});
    var base64Img = "";
    var reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = function () {
        base64Img = reader.result; //获取到图片的base64编码
        imgArr.push(base64Img);//放入数组
        imgArrType.push(fileType);
        toImgList();//刷新数组 展示到页面
        document.getElementById('toFileId').value = "";//清掉刚选择的图片信息 不然不能连续选两张同样的文件
    };
    count++;
}


/**
 * 加载图片列表
 */
function toImgList() {
    //删除图标
    var ico = "<svg class=\"icon\" style=\"width: 1.5em; height: 1.5em;vertical-align: middle;fill: currentColor;overflow: hidden;\" viewBox=\"0 0 1024 1024\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" p-id=\"1612\" data-spm-anchor-id=\"a313x.7781069.1998910419.i3\"><path d=\"M512 0C227.84 0 0 227.84 0 512s227.84 512 512 512c284.096 0 512-227.84 512-512S796.096 0 512 0L512 0zM768 696.32 696.256 768 512 583.68 327.616 768 256 696.32 440.256 512 256 327.68 327.616 256 512 440.32 696.256 256 768 327.68 583.616 512 768 696.32 768 696.32zM768 696.32\" p-id=\"1613\"></path></svg>";

    var textHtml = "";
    for (var i = 0; i < imgArr.length; i++) {
        textHtml += " <div style='    float: left;width: 150px;height: 150px; margin-left: 10px;position: relative;'> " +
            "<img  style='width: 100%; height: auto' src=" + imgArr[i] + ">" +
            "<a onclick='deleteImg(" + i + ")' style='top:5px;right:5px;display:block; position:absolute;'>" + ico + "</a>" +
            "</div>";
    }
    $("#imgList").html(textHtml);
}

/**
 * 根据索引删除图片
 * @param i
 */
function deleteImg(i) {
    imgArr.splice(i, 1);
    imgArrType.splice(i, 1);
    toImgList();
    count--;
}

/**
 base64编码转file对象
 */
function baseToFile(base, type) {
    var img = base.split(',')[1];
    img = window.atob(img);
    var ia = new Uint8Array(img.length);
    for (var j = 0; j < img.length; j++) {
        ia[j] = img.charCodeAt(j);
    }
    ;
    //文件重命名
    var file = new File([ia], $("#userId").val() + "_" + Math.round(new Date()), {type: "image/" + type});
    return file;
}

/**
 * 第三步上传图片 提交
 */

function buttonImg() {

    var jsonObject = {};
    jsonObject.id = 1;
    jsonObject.name = "name";
    var toJson = JSON.stringify(jsonObject);

    var formdata = new FormData();
    formdata.append('toJson', toJson);

    for (var i = 0; i < imgArr.length; i++) {
        var fileBlob = baseToFile(imgArr[i], imgArrType[i]);
        formdata.append('fileBlob', fileBlob);
    }

    $.ajax({
        url: "/uploadImg", //请求地址
        data: formdata, //参数
        type: "POST",
        dataType: "text", //返回格式
        processData: false,
        contentType: false,
        cache: false,
        success: function (data) {
            $("#filePathDiv").html(data);

        },
        error: function (e) {
        }
    });
}

//返回顶部
$(function () {
    var bt = $('#toolBackTop');
    var sw = $(document.body)[0].clientWidth;

    var limitsw = (sw - 840) / 2 - 80;  //距离右侧距离
    if (limitsw > 0) {
        limitsw = parseInt(limitsw);
        bt.css("right", limitsw / 8);
    }

    $(window).scroll(function () {
        var st = $(window).scrollTop();
        if (st > 30) {
            bt.show();
        } else {
            bt.hide();
        }
    });
});

//取消选择视频
document.getElementById("clearVideo").addEventListener('click', function () {
    document.getElementById("videoId").value = '';
});
