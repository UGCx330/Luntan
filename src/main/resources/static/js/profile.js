var userId = $("#userId").val();
$(function () {
    $(".follow-btn").click(follow);
});

function follow() {
    var btn = this;
    if ($(btn).hasClass("btn-info")) {
        $.post(
            CONTEXT_PATH + "/follow",
            {"entityType": 3, "entityId": $(btn).prev().val()},
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
        $.post(
            CONTEXT_PATH + "/deFollow",
            {"entityType": 3, "entityId": $(btn).prev().val()},
            function (data) {
                data = $.parseJSON(data);
                if (data.code == 0) {
                    window.location.reload();
                } else {
                    alert(data.msg);
                }
            }
        );
    }

}

//-----------------------------------------------------------------------------------------相册
if (document.getElementsByName("img").length !== 0) {
    // 获取外层容器
    const shell = document.querySelector('.boxs');
// 获取所有子元素
    const cells = shell.querySelectorAll('.box');
// 获取容器宽度
    const cellWidth = shell.offsetWidth;
// 获取容器高度
    const cellHeight = shell.offsetHeight;
// 设置子元素大小为容器高度
    const cellSize = cellHeight;
// 动态获取子元素数量
    const cellCount = document.getElementsByName("img").length;
// 计算半径
    const radius = Math.round((cellSize / 1.8) / Math.tan(Math.PI / cellCount));
// 计算每个子元素的角度
    const theta = 360 / cellCount;
// 当前选中的子元素索引
    let selectedIndex = 0;

    function rotateshell() {
        // 计算旋转角度
        const angle = theta * selectedIndex * -1;
        // 设置容器的旋转和平移效果
        shell.style.transform = 'translateZ(' + -radius + 'px) ' + 'rotateX(' + -angle + 'deg)';
        // 计算当前选中的子元素索引
        const cellIndex = selectedIndex < 0 ? (cellCount - ((selectedIndex * -1) % cellCount)) : (selectedIndex % cellCount);
        cells.forEach((cell, index) => {
            if (cellIndex === index) {
                // 添加选中样式
                cell.classList.add('selected');
            } else {
                // 移除选中样式
                cell.classList.remove('selected');
            }
        });
    }

    function selectPrev() {
        // 选中上一个子元素
        selectedIndex--;
        // 旋转容器
        rotateshell();
    }

    function selectNext() {
        // 选中下一个子元素
        selectedIndex++;
        // 旋转容器
        rotateshell();
    }

// 获取上一个按钮
    const prevButton = document.querySelector('.up');
// 绑定点击事件
    prevButton.addEventListener('click', selectPrev);
// 获取下一个按钮
    const nextButton = document.querySelector('.next');
// 绑定点击事件
    nextButton.addEventListener('click', selectNext);

    function initshell() {
        cells.forEach((cell, i) => {
            // 计算每个子元素的角度
            const cellAngle = theta * i;
            // 设置每个子元素的旋转和平移效果
            cell.style.transform = 'rotateX(' + -cellAngle + 'deg) translateZ(' + radius + 'px)';
        });
        // 初始化旋转容器
        rotateshell();
    }

// 调用初始化函数
    initshell();

}

//------------------------------------------------------------------------------------------------上传相册

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
    if (count >= 6) {
        alert("存储紧张，暂时只支持上传六张图片~");
        return false;
    }
    var file = files.files[0];
    //获取文件后缀
    var fileType = file.name.substring(file.name.lastIndexOf(".") + 1, file.name.length);

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
    var file = new File([ia], userId + "_" + Math.round(new Date()), {type: "image/" + type});
    return file;
}

$(function () {
    $("#imgPublishBtn").click(imgPublish);
});

function imgPublish() {
    if (count === 1 || count === 2) {
        alert("最少上传3张");
        return false;
    }
    $("#publishModal").modal("hide");
    //最终给服务器的图片名字
    var fileNames = "";
    var tempName = "";
    var formData;
    //循环上传图片
    for (var i = 0; i < imgArr.length; i++) {
        var file = baseToFile(imgArr[i], imgArrType[i]);
        formData = new FormData();
        formData.append('token', $("#imgUploadToken").val());
        tempName = userId + "_" + Math.round(new Date());
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

    //更新数据库中图片路径
    $.post(
        CONTEXT_PATH + "/user/updateProfileImg",
        {
            "userId": userId,
            "fileNames": fileNames,
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

//----------------------------------------------------------------------------------------------------音乐播放器
const musicContainer = document.getElementById("music-container");
const playBtn = document.getElementById("play");
const prevBtn = document.getElementById("prev");
const nextBtn = document.getElementById("next");

const audio = document.getElementById("audio");
const progress = document.getElementById("progress");
const progressContainer = document.getElementById("progress-container");
const title = document.getElementById("title");
const musicCover = document.getElementById("music-cover");

// 音乐信息,使用musicList
// 默认从第一首开始
if (songs != null) {
    let songIndex = 0;
// 将歌曲细节加载到DOM
    loadSong(songs[songIndex]);

// 更新歌曲细节,song此时是一个map
    function loadSong(song) {
        title.innerHTML = song["musicName"];
        audio.src = song["qiniuMusic"];
        musicCover.src = song["qiniuMusicImg"];
    }

// 播放歌曲
    function playSong() {
        // 元素添加类名
        musicContainer.classList.add("play")
        playBtn.querySelector('i.fas').classList.remove('fa-play')
        playBtn.querySelector('i.fas').classList.add('fa-pause')

        audio.play()
    }

// 停止播放
    function pauseSong() {
        musicContainer.classList.remove('play');
        playBtn.querySelector('i.fas').classList.add('fa-play');
        playBtn.querySelector('i.fas').classList.remove('fa-pause');

        audio.pause();
    }

// 上一首
    function prevSong() {
        songIndex--
        if (songIndex < 0) {
            songIndex = songs.length - 1
        }
        // 加载歌曲信息并播放
        loadSong(songs[songIndex])
        playSong()
    }

// 下一首
    function nextSong() {
        songIndex++;

        if (songIndex > songs.length - 1) {
            songIndex = 0;
        }

        loadSong(songs[songIndex]);

        playSong();
    }

// 进度条更新
    function updateProgress(e) {
        // audio.duration: 音频长度
        // audio.currentTime: 音频播放位置
        // 对象解构操作
        const {
            duration,
            currentTime
        } = e.target;
        // e.target = {
        //     duration: 225,  // 当前音频时间长度
        //     currentTime:0  // 当前播放时间
        // }
        const progressPercent = (currentTime / duration) * 100
        // 进度条
        progress.style.width = `${progressPercent}%`
    }

// 设置进度条
    function setProgress(e) {
        // progressContainer代理视图宽度
        const width = this.clientWidth
        // 鼠标点击时处于progressContainer里的水平偏移量
        const clickX = e.offsetX

        // audio.duration: 音频长度
        const duration = audio.duration

        // audio.currentTime: 音频播放位置
        audio.currentTime = (clickX / width) * duration
    }

// 事件监听
// 1.播放歌曲
    playBtn.onclick = function () {
        // 判断当前是否是正在播放

        // const isPlaying = musicContainer.classList.contains('play')
        // if (isPlaying) {
        //     pauseSong()
        // } else {
        //     playSong()
        // }
        musicContainer.classList.contains('play') ? pauseSong() : playSong()
    }
// 2.切换歌曲
    prevBtn.onclick = prevSong
    nextBtn.onclick = nextSong

// 3.播放器进度条相关
// 3.1 设置播放进度
    progressContainer.onclick = setProgress
// 3.2 进度条更新
    audio.ontimeupdate = updateProgress
// 3.3 歌曲结束后自动下一首
    audio.onended = nextSong
}

//-------------------------------------------------------------------------------------------上传音乐
//获取一个上传文件的扩展名
var music = document.getElementById('musicFile');
var musicName = "";
music.addEventListener("change", function () {
    if (music.files.length > 3) {
        alert("存储紧张~~最多上传三首~");
        document.getElementById('musicFile').value = '';
        document.getElementById("musicNames").value = '';
        return false;
    }
    //获取上传文件的文件名
    for (let i = 0; i < music.files.length; i++) {
        musicName += music.files[i].name + "，";
    }
    document.getElementById("musicNames").value = musicName;
    musicName = "";
});

var musicImg = document.getElementById('musicImgFile');
var musicImgName = "";
musicImg.addEventListener("change", function () {
    if (musicImg.files.length > 3) {
        alert("存储紧张~~最多上传三张~");
        document.getElementById('musicImgFile').value = '';
        document.getElementById("musicImgNames").value = '';
        return false;
    }
    //获取上传文件的文件名
    for (let i = 0; i < musicImg.files.length; i++) {
        musicImgName += musicImg.files[i].name + "，";
    }
    document.getElementById("musicImgNames").value = musicImgName;
    musicImgName = "";
});

function delMusic() {
    document.getElementById('musicFile').value = '';
    document.getElementById("musicNames").value = '';
    document.getElementById('musicImgFile').value = '';
    document.getElementById("musicImgNames").value = '';
}


// 上传七牛云
$(function () {
    $("#musicPublishBtn").click(musicPublish);
});

function musicPublish() {
    $("#publishMusicModal").modal("hide");
    var musicLength = music.files.length;
    var imgLength = musicImg.files.length;
    var minLength = musicLength <= imgLength ? musicLength : imgLength;
    var sqlName = '';
    var formData;
    var tmpTime = '';
    if (musicLength === 0 && imgLength !== 0) {
        alert("未选择任何音乐，请清除封面图");
        return false;
    }
    for (let i = 0; i < minLength; i++) {
        tmpTime = userId + "_" + Math.round(new Date());
        //音乐与封面图前缀部分共用，使用~分割三首音乐及其封面
        sqlName += tmpTime + ":" + music.files[i].name + "~";
        //上传到一个仓库里
        //先上传音乐,统一添加后缀Music
        formData = new FormData();
        formData.append('token', $("#musicUploadToken").val());
        formData.append('key', tmpTime + "Music");
        formData.append('file', music.files[i]);
        //上传音乐到七牛云
        $.ajax(
            {
                url: "http://upload-z1.qiniup.com",
                method: "post",
                processData: false,
                contentType: false,
                data: formData,
                success: function (data) {
                    if (data.code != 0) {
                        alert("上传音乐超时，请稍后再试");
                        //一旦出错立刻终结上传
                        return;
                    }
                }
            }
        );
        //上传封面图到七牛云
        formData = new FormData();
        formData.append('token', $("#musicUploadToken").val());
        formData.append('key', tmpTime + "MusicImg");
        formData.append('file', musicImg.files[i]);
        $.ajax(
            {
                url: "http://upload-z1.qiniup.com",
                method: "post",
                processData: false,
                contentType: false,
                data: formData,
                success: function (data) {
                    if (data.code != 0) {
                        alert("上传封面图超时，请稍后再试");
                        //一旦出错立刻终结上传
                        return;
                    }
                }
            }
        );
    }

    //将所有音乐和封面图以及音乐的名字存入数据库
    $.post(
        CONTEXT_PATH + "/user/updateProfileMusic",
        {
            "userId": userId,
            "fileName": sqlName,
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

//------------------------------------------------------------------------------------------上传视频
$(function () {
    $("#videoPublishBtn").click(videoPublish);
});

function videoPublish() {
    $("#publishVideoModal").modal("hide");
    //最终给服务器的视频名字
    var fileName = "";
    var video = $("#video")[0].files[0];
    if (video != null) {
        var formData;
        formData = new FormData();
        formData.append('token', $("#videoUploadToken").val());
        fileName = userId + "_" + Math.round(new Date());
        formData.append('key', fileName);
        //这里获取video必须使用document.getElementById("video").files[0]或者以下方法
        // $("#video")files[0]写法不对，因为jq选择器得到的是jq对象，而不是dom对象，使用$("#video")[0].files[0]可以将jq对象转为dom对象
        //jq选择器生成的jq对象本质上就是一个数组，数组的下标0位置就是原生的DOM对象。所以上述才会这样写。
        formData.append('file', video);
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
                        alert("上传视频超时，请稍后再试");
                        //一旦出错立刻终结上传
                        return;
                    }
                }
            }
        );
    }
    //更新数据库中视频路径
    $.post(
        CONTEXT_PATH + "/user/updateProfileVideo",
        {
            "userId": userId,
            "fileName": fileName,
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
//------------------------------------------------------------------------------------------上传背景图
$(function () {
    $("#backPublishBtn").click(backPublish);
});

function backPublish() {
    $("#publishBackModal").modal("hide");
    //最终给服务器的视频名字
    var fileName = "";
    var back = $("#back")[0].files[0];
    if (back != null) {
        var formData;
        formData = new FormData();
        formData.append('token', $("#backUploadToken").val());
        fileName = userId + "_" + Math.round(new Date());
        formData.append('key', fileName);
        //这里获取video必须使用document.getElementById("video").files[0]或者以下方法
        // $("#video")files[0]写法不对，因为jq选择器得到的是jq对象，而不是dom对象，使用$("#video")[0].files[0]可以将jq对象转为dom对象
        //jq选择器生成的jq对象本质上就是一个数组，数组的下标0位置就是原生的DOM对象。所以上述才会这样写。
        formData.append('file', back);
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
                        alert("上传视频超时，请稍后再试");
                        //一旦出错立刻终结上传
                        return;
                    }
                }
            }
        );
    }
    //更新数据库中视频路径
    $.post(
        CONTEXT_PATH + "/user/updateProfileBack",
        {
            "userId": userId,
            "fileName": fileName,
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