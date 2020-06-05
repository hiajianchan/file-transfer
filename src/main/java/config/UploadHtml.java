package config;

/**
 * @author chenhaijian
 * @date 2020-05-22 10:55
 */
public class UploadHtml {

    public static final String HTML = "<html>\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<script src=\"https://cdn.bootcss.com/jquery/3.4.1/jquery.js\"></script>\n" +
            "<title>Untitled Document</title>\n" +
            "</head>\n" +
            "\n" +
            "<body>\n" +
            "    <div>\n" +
            "        <div class=\"row\" style=\"border-top: 0.0rem;margin-bottom: 0.15rem;padding-top: 0.0rem\">\n" +
            "            <div class=\"upload_container\" style=\"display: flex; justify-content: flex-start;\">\n" +
            "                <label class=\"el-form-item__label\">选择文件:</label>\n" +
            "                <div class=\"upload_input\" id=\"upload\" style=\"margin-left: 250px\">\n" +
            "                    <button type=\"button\" class=\"btn_default query_search_btn import_btn\" onclick=\"showSelectFileWin();\">Choose File</button>\n" +
            "                    <span id=\"showFileName\" class=\"file_name_show\">No file chosen</span>\n" +
            "                    <span id=\"sonValue\"></span>\n" +
            "                    <span id=\"sonProcess\"></span>\n" +
            "                    <input type=\"file\" onchange=\"onChangeFile()\" id=\"file\" class=\"upload_input_area\" style=\" display: none\" name=\"file\"/>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
//            "<input type=\"button\" onclick=\"uploadFormCancel()\" value=\"取消\"/>\n" +
            "<input type=\"button\" onclick=\"uploadFile()\" value=\"上传\"/>\n" +
            "<script>\n" +
            "    var vm = Object;\n" +
            "    /**\n" +
            "     * 选择文件事件\n" +
            "     * @return {boolean}\n" +
            "     */\n" +
            "    function onChangeFile() {\n" +
            "        $(\"#showFileName\").html(\"未选择文件\");\n" +
            "        $(\"#showFileName\").removeAttr(\"title\");\n" +
            "        var file = $(\"#file\")[0].files[0];\n" +
            "        $(\"#showFileName\").attr(\"title\", file.name);\n" +
            "        $(\"#showFileName\").html(file.name);\n" +
            "    } \n" +
            "    /**\n" +
            "     * 上传选择的文件\n" +
            "     * @param event\n" +
            "     */\n" +
            "    function uploadFile () {\n" +
            "        if ($(\"#showFileName\").text() == \"No file chosen\") {\n" +
            "             alert('请选择文件！');" +
            "         } else {\n" +
            "            var file = $(\"#file\")[0].files[0];\n" +
            "            var formData = new FormData();\n" +
            "            formData.append('file', file);\n" +
            "            var url = \"http://{#ip}:{#port}/post_multipart?passwd={#passwd}\"\n" +
            "            $.ajax({\n" +
            "                type: \"POST\",\n" +
            "                url: url,\n" +
            "                data: formData,\n" +
            "                dataType: \"json\",\n" +
            "                cache: false,//上传文件无需缓存\n" +
            "                processData: false,//用于对data参数进行序列化处理 这里必须false\n" +
            "                contentType: false, //必须\n" +
            "                xhr: function () {\n" +
            "                    //获取xmlhttprequest对象或者ActiveXObject 对象，jquery中封装好的。\n" +
            "                    var xhr = $.ajaxSettings.xhr();\n" +
            "                    if (xhr.upload) {\n" +
            "                    xhr.upload.addEventListener(\"progress\", progressBar, false);\n" +
            "                    }\n" +
            "                    return xhr;\n" +
            "\n" +
            "                },\n" +
            "                success: function (response) {\n" +
            "                    if (response.code == 200) {\n" +
            "                        alert(response.msg);\n" +
            "                        reset();" +
            "                    } else {\n" +
            "                        alert('系统错误！请稍后再试！');\n" +
            "                        reset();" +
            "                    }\n" +
            "                },\n" +
            "                error: function (response) {\n" +
            "                    alert('系统错误！请稍后再试！');\n" +
            "                    reset();" +
            "                }\n" +
            "\n" +
            "            });\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    function progressBar(evt) {\n" +
            "        var loaded = evt.loaded; //已经上传大小情况\n" +
            "        var tot = evt.total; //附件总大小\n" +
            "        var per = Math.floor(100 * loaded / tot); //已经上传的百分比\n" +
            "        $(\"#sonValue\").html(per + \"%\");\n" +
            "        $(\"#sonProcess\").width(per + \"%\");\n" +
            "      }\n" +
            "\n" +
            "    /** * 显示选择文件的窗口 */\n" +
            "    function showSelectFileWin() {    \n" +
            "        $(\"#file\").val(\"\");    $(\"#file\").click();\n" +
            "    }\n" +
            "/**\n" +
            "    *  重置 \n" +
            "    */\n" +
            "    function reset() {\n" +
            "        $(\"#showFileName\").html('未选择文件');\n" +
            "        $(\"#file\").val(\"\");\n" +
            "        $(\"#sonValue\").html('');\n" +
            "        $(\"#sonProcess\").width('');\n" +
            "    }" +
            "</script>\n" +
            "</body>\n" +
            "</html>\n";
}
