package org.javamaster.httpclient.impl.dashboard;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.httpClient.localize.HttpClientLocalize;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.process.BaseProcessHandler;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.NotificationType;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.lang.StringUtil;
import consulo.util.lang.Trinity;
import consulo.virtualFileSystem.VirtualFileManager;
import org.javamaster.httpclient.NlsBundle;
import org.javamaster.httpclient.env.EnvFileService;
import org.javamaster.httpclient.impl.background.HttpBackground;
import org.javamaster.httpclient.impl.js.JsExecutor;
import org.javamaster.httpclient.impl.resolve.VariableResolver;
import org.javamaster.httpclient.impl.ui.HttpDashboardForm;
import org.javamaster.httpclient.impl.utils.HttpUtils;
import org.javamaster.httpclient.impl.utils.NotifyUtil;
import org.javamaster.httpclient.map.LinkedMultiValueMap;
import org.javamaster.httpclient.model.*;
import org.javamaster.httpclient.parser.HttpFile;
import org.javamaster.httpclient.psi.*;
import org.javamaster.httpclient.utils.HttpUtilsPart;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

/**
 * @author yudong
 */
public class HttpProcessHandler extends BaseProcessHandler {
    public final HttpMethod httpMethod;
    private final String selectedEnv;
    public final String tabName;
    public final Project project;
    public Integer httpStatus;
    public Long costTimes;

    private final HttpFile httpFile;
    private final String parentPath;
    private final JsExecutor jsExecutor;
    private final VariableResolver variableResolver;
    private final Runnable loadingRemover;
    private final HttpRequestTarget requestTarget;
    private final HttpRequest request;
    private final HttpRequestBlock requestBlock;
    private final HttpRequestEnum methodType;
    private final HttpResponseHandler responseHandler;

    private final List<org.javamaster.httpclient.model.PreJsFile> preJsFiles;
    private final List<HttpScriptBody> jsListBeforeReq;
    private final HttpScriptBody jsAfterReq;
    private final Map<String, String> paramMap;

    private HttpDashboardForm httpDashboardForm;

    private final consulo.http.HttpVersion version;
    //private WsRequest wsRequest;
    //private ServerSocket serverSocket;

    public boolean hasError = false;

    @RequiredReadAction
    public HttpProcessHandler(HttpMethod httpMethod, String selectedEnv) {
        this.httpMethod = httpMethod;
        this.selectedEnv = selectedEnv;
        this.tabName = HttpUtilsPart.getTabName(httpMethod);
        this.project = httpMethod.getProject();

        this.httpFile = (HttpFile) httpMethod.getContainingFile();
        this.parentPath = httpFile.getVirtualFile().getParent().getPath();
        this.jsExecutor = JsExecutor.create(project, httpFile, tabName);
        this.variableResolver = new VariableResolver(jsExecutor, httpFile, selectedEnv, project);
        this.loadingRemover = httpMethod.getUserData(HttpUtils.gutterIconLoadingKey);
        this.requestTarget = PsiTreeUtil.getNextSiblingOfType(httpMethod, HttpRequestTarget.class);
        this.request = PsiTreeUtil.getParentOfType(httpMethod, HttpRequest.class);
        this.requestBlock = PsiTreeUtil.getParentOfType(request, HttpRequestBlock.class);
        this.methodType = HttpRequestEnum.getInstance(httpMethod.getText());
        this.responseHandler = PsiTreeUtil.getChildOfType(request, HttpResponseHandler.class);

        this.preJsFiles = HttpUtils.getPreJsFiles(httpFile, false);
        this.jsListBeforeReq = HttpUtils.getAllPreJsScripts(httpFile, requestBlock);
        this.jsAfterReq = HttpUtils.getJsScript(responseHandler);
        this.paramMap = HttpUtils.getReqDirectionCommentParamMap(requestBlock);

        HttpVersion httpVersion = request.getVersion();
        this.version = httpVersion != null ? httpVersion.getVersion() : consulo.http.HttpVersion.HTTP_1_1;
    }

    public JPanel getComponent() {
        if (httpDashboardForm == null) {
            httpDashboardForm = new HttpDashboardForm(tabName, project);
        }
        return httpDashboardForm.getMainPanel();
    }

    @Override
    public void startNotify() {
        super.startNotify();

        if (preJsFiles.isEmpty()) {
            startRequest();
            return;
        }

        //initNpmFilesThenStartRequest();
    }

//    private void initNpmFilesThenStartRequest() {
//        Map<Boolean, List<org.javamaster.httpclient.model.PreJsFile>> preFilePair =
//            preJsFiles.stream().collect(java.util.stream.Collectors.partitioningBy(
//                it -> it.getUrlFile() != null
//            ));
//
//        List<org.javamaster.httpclient.model.PreJsFile> npmFiles = preFilePair.get(true);
//
//        if (npmFiles.isEmpty()) {
//            initPreFilesThenStartRequest();
//            return;
//        }
//
//        List<org.javamaster.httpclient.model.PreJsFile> npmFilesNotDownloaded =
//            JsTgz.jsLibrariesNotDownloaded(npmFiles);
//
//        if (!npmFilesNotDownloaded.isEmpty()) {
//            JsTgz.downloadAsync(project, npmFilesNotDownloaded, null);
//            destroyProcess();
//            return;
//        }
//
//        Application.get().executeOnPooledThread(() -> {
//            Application.get().runReadAction(() -> {
//                JsTgz.initAndCacheNpmJsLibrariesFile(npmFiles, project);
//                initPreFilesThenStartRequest();
//            });
//        });
//    }

//    private void initPreFilesThenStartRequest() {
//        Application.get().executeOnPooledThread(() -> {
//            JsTgz.initJsLibrariesVirtualFile(preJsFiles);
//
//            Application.get().invokeLater(this::startRequest);
//        });
//    }

//    private void initPreJsFilesContent() {
//        preJsFiles.forEach(it -> {
//            try {
//                String content = VirtualFileUtils.readNewestContent(it.getVirtualFile());
//                it.setContent(content);
//            }
//            catch (Exception e) {
//                Document document =
//                    PsiDocumentManager.getInstance(project).getDocument(httpFile);
//                int rowNum = document.getLineNumber(it.getDirectionComment().getTextOffset()) + 1;
//
//                throw new RuntimeException(e + "(" + httpFile.getName() + "#" + rowNum + ")", e);
//            }
//        });
//    }

    private void startRequest() {
        HttpBackground
            .runInBackgroundReadActionAsync(() -> {
                //initPreJsFilesContent();

                Object reqBody = HttpUtils.convertToReqBody(request, variableResolver, paramMap);

                String environment = HttpUtils.gson.toJson(EnvFileService.getEnvMap(project, false));

                return new HttpReqInfo(reqBody, environment, preJsFiles);
            })
            .finishOnUiThread(reqInfo -> startHandleRequest(reqInfo))
            .exceptionallyOnUiThread(this::handleException);
    }

    private void startHandleRequest(HttpReqInfo reqInfo) {
        HttpHeader header = request.getHeader();
        List<HttpHeaderField> httpHeaderFields = header != null ? header.getHeaderFieldList() : null;

        LinkedMultiValueMap<String, String> reqHeaderMap =
            HttpUtils.convertToReqHeaderMap(httpHeaderFields, variableResolver);

        jsExecutor.initJsRequestObj(
            reqInfo,
            methodType,
            reqHeaderMap,
            selectedEnv,
            variableResolver.getFileScopeVariableMap()
        );

        List<String> beforeJsResList = jsExecutor.evalJsBeforeRequest(reqInfo.getPreJsFiles(), jsListBeforeReq);

        java.util.List<String> httpReqDescList = new java.util.ArrayList<>();
        httpReqDescList.addAll(beforeJsResList);

        String url = variableResolver.resolve(requestTarget.getUrl());

        if (paramMap.containsKey(ParamEnum.AUTO_ENCODING.getParam())) {
            url = HttpUtils.encodeUrl(url);
        }

        reqHeaderMap = HttpUtils.resolveReqHeaderMapAgain(reqHeaderMap, variableResolver);

        reqHeaderMap.putAll(jsExecutor.getHeaderMap());

        Object reqBody = reqInfo.getReqBody();

        switch (methodType) {
//            case WEBSOCKET:
//                handleWs(url, reqHeaderMap);
//                break;
//            case DUBBO:
//                handleDubbo(url, reqHeaderMap, reqBody, httpReqDescList);
//                break;
//            case MOCK_SERVER:
//                handleMockServer();
//                break;
            default:
                handleHttp(project, url, reqHeaderMap, reqBody, httpReqDescList);
                break;
        }
    }

//    private void handleMockServer() {
//        if (loadingRemover != null) {
//            loadingRemover.run();
//        }
//
//        MockServer mockServer = new MockServer();
//
//        if (httpDashboardForm == null) {
//            httpDashboardForm = new HttpDashboardForm(tabName, project);
//        }
//        httpDashboardForm.initMockServerForm(mockServer);
//
//        serverSocket = mockServer.startServerAsync(request, variableResolver, paramMap);
//    }

//    public void prepareJsAndConvertToCurl(boolean raw, Consumer<String> consumer) {
//        Map<Boolean, List<org.javamaster.httpclient.model.PreJsFile>> preFilePair =
//            preJsFiles.stream().collect(java.util.stream.Collectors.partitioningBy(
//                it -> it.getUrlFile() != null
//            ));
//
//        List<org.javamaster.httpclient.model.PreJsFile> npmFiles = preFilePair.get(true);
//
//        if (npmFiles.isEmpty()) {
//            convertToCurl(raw, consumer);
//            return;
//        }
//
//        List<org.javamaster.httpclient.model.PreJsFile> npmFilesNotDownloaded =
//            JsTgz.jsLibrariesNotDownloaded(npmFiles);
//
//        if (!npmFilesNotDownloaded.isEmpty()) {
//            JsTgz.downloadAsync(project, npmFilesNotDownloaded, () -> {
//                Application.get().executeOnPooledThread(() -> {
//                    Application.get().runReadAction(() -> {
//                        JsTgz.initAndCacheNpmJsLibrariesFile(npmFiles, project);
//                        convertToCurl(raw, consumer);
//                    });
//                });
//            });
//            return;
//        }
//
//        Application.get().executeOnPooledThread(() -> {
//            Application.get().runReadAction(() -> {
//                JsTgz.initAndCacheNpmJsLibrariesFile(npmFiles, project);
//                convertToCurl(raw, consumer);
//            });
//        });
//    }

//    private void convertToCurl(boolean raw, Consumer<String> consumer) {
//        Application.get().executeOnPooledThread(() -> {
//            JsTgz.initJsLibrariesVirtualFile(preJsFiles);
//
//            Application.get().invokeLater(() -> {
//                convertToCurlReal(raw, consumer);
//            });
//        });
//    }
//
//    private void convertToCurlReal(boolean raw, Consumer<String> consumer) {
//        HttpBackground
//            .runInBackgroundReadActionAsync(() -> {
//                initPreJsFilesContent();
//
//                Object reqBody = HttpUtils.convertToReqBody(request, variableResolver, paramMap);
//
//                String environment = HttpUtils.gson.toJson(EnvFileService.getEnvMap(project, false));
//
//                return new HttpReqInfo(reqBody, environment, preJsFiles);
//            })
//            .finishOnUiThread(reqInfo -> {
//                convertToCurlRealWithInfo(raw, consumer, reqInfo);
//            })
//            .exceptionallyOnUiThread(throwable -> {
//                NotifyUtil.notifyError(project, throwable.toString());
//            });
//    }

//    private void convertToCurlRealWithInfo(boolean raw, Consumer<String> consumer, HttpReqInfo reqInfo) {
//        HttpHeader header = request.getHeader();
//        List<HttpHeaderField> httpHeaderFields = header != null ? header.getHeaderFieldList() : null;
//
//        LinkedMultiValueMap<String, String> reqHeaderMap =
//            HttpUtils.convertToReqHeaderMap(httpHeaderFields, variableResolver);
//
//        jsExecutor.initJsRequestObj(
//            reqInfo,
//            methodType,
//            reqHeaderMap,
//            selectedEnv,
//            variableResolver.getFileScopeVariableMap()
//        );
//
//        List<String> resList = jsExecutor.evalJsBeforeRequest(reqInfo.getPreJsFiles(), jsListBeforeReq);
//        System.out.println("js执行结果:" + resList);
//
//        String url = variableResolver.resolve(requestTarget.getUrl());
//
//        if (paramMap.containsKey(ParamEnum.AUTO_ENCODING.getParam())) {
//            url = HttpUtils.encodeUrl(url);
//        }
//
//        reqHeaderMap = HttpUtils.resolveReqHeaderMapAgain(reqHeaderMap, variableResolver);
//
//        reqHeaderMap.putAll(jsExecutor.getHeaderMap());
//
//        java.util.List<String> list = new java.util.ArrayList<>();
//
//        if (raw) {
//            String tabNameLocal = HttpUtils.getTabName(request.getMethod());
//            list.add("### " + tabNameLocal + HttpUtils.CR_LF);
//        }
//
//        list.add(
//            raw ?
//                request.getMethod().getText() + " " + url + HttpUtils.CR_LF :
//                "curl -X " + request.getMethod().getText() + " --location \"" + url + "\""
//        );
//
//        reqHeaderMap.forEach((name, values) -> {
//            for (String value : values) {
//                list.add(
//                    raw ?
//                        name + ": " + value + HttpUtils.CR_LF :
//                        "    -H \"" + name + ": " + value + "\""
//                );
//            }
//        });
//
//        if (raw) {
//            list.add(HttpUtils.CR_LF);
//        }
//
//        HttpBackground.runInBackgroundReadActionAsync(() -> {
//            HttpHeader headerInner = request.getHeader();
//            HttpBody body = request.getBody();
//            HttpRequestMessagesGroup requestMessagesGroup = body != null ? body.getRequestMessagesGroup() : null;
//            HttpMultipartMessage httpMultipartMessage = body != null ? body.getMultipartMessage() : null;
//
//            if (requestMessagesGroup != null) {
//                String content = HttpUtils.handleOrdinaryContentCurl(requestMessagesGroup, variableResolver, headerInner, raw);
//
//                list.add(
//                    raw ?
//                        content :
//                        "    -d '" + content + "'"
//                );
//            }
//            else if (httpMultipartMessage != null) {
//                String boundary = request.getContentTypeBoundary();
//                if (boundary == null) {
//                    boundary = HttpUtils.WEB_BOUNDARY;
//                }
//
//                List<String> contents = HttpUtils.constructMultipartBodyCurl(httpMultipartMessage, variableResolver, boundary, raw);
//
//                list.addAll(contents);
//            }
//
//            if (raw) {
//                return String.join("", list);
//            }
//            else {
//                return String.join(" \\" + HttpUtils.CR_LF, list);
//            }
//        }).finishOnUiThread(result -> {
//            consumer.accept(result);
//        }).exceptionallyOnUiThread(throwable -> {
//            NotifyUtil.notifyError(project, throwable.toString());
//        });
//    }

    private void handleException(Exception e) {
        destroyProcess();
        NotifyUtil.notifyError(project, "<div style='font-size:13pt'>" + e + "</div>");
    }

//    private void handleWs(String url, LinkedMultiValueMap<String, String> reqHeaderMap) {
//        if (loadingRemover != null) {
//            loadingRemover.run();
//        }
//
//        if (httpDashboardForm == null) {
//            httpDashboardForm = new HttpDashboardForm(tabName, project);
//        }
//
//        wsRequest = new WsRequest(url, reqHeaderMap, this, paramMap, httpDashboardForm);
//
//        httpDashboardForm.initWsForm(wsRequest);
//
//        wsRequest.connect();
//    }

//    private void handleDubbo(
//        String url,
//        LinkedMultiValueMap<String, String> reqHeaderMap,
//        Object reqBody,
//        java.util.List<String> httpReqDescList
//    ) {
//        if (DubboJars.jarsNotDownloaded()) {
//            DubboJars.downloadAsync(project);
//            destroyProcess();
//            return;
//        }
//
//        DubboHandler dubboRequest = ActionUtil.underModalProgress(project, "Processing dubbo...", () -> {
//            Module module = ModuleUtil.findModuleForPsiElement(httpFile);
//
//            String clsName = "org.javamaster.httpclient.dubbo.DubboRequest";
//            Class<?> dubboRequestClazz = DubboJars.dubboClassLoader.loadClass(clsName);
//
//            java.lang.reflect.Constructor<?> constructor = dubboRequestClazz.getDeclaredConstructors()[0];
//            constructor.setAccessible(true);
//
//            DubboHandler dubboRequestInstance;
//            try {
//                dubboRequestInstance = (DubboHandler) constructor.newInstance(
//                    tabName, url, reqHeaderMap, reqBody,
//                    httpReqDescList, module, project, paramMap
//                );
//            } catch (InvocationTargetException e) {
//                throw e.getTargetException();
//            }
//
//            return dubboRequestInstance;
//        });
//
//        CompletableFuture<org.javamaster.httpclient.utils.Pair<byte[], Long>> future = dubboRequest.sendAsync();
//
//        future.whenCompleteAsync((pair, throwable) -> {
//            Application.get().invokeLater(() -> {
//                Application.get().runWriteAction(() -> {
//                    if (throwable != null) {
//                        HttpInfo info = new HttpInfo(httpReqDescList, new java.util.ArrayList<>(), null, null, throwable);
//                        dealResponse(info, parentPath);
//                        return;
//                    }
//
//                    httpStatus = 200;
//                    costTimes = pair.getSecond();
//
//                    byte[] byteArray = pair.getFirst();
//                    Long consumeTimes = pair.getSecond();
//
//                    String size = Formats.formatFileSize(byteArray.length);
//
//                    String comment = NlsBundle.message("res.desc", 200, consumeTimes, size);
//
//                    java.util.List<String> httpResDescList = new java.util.ArrayList<>();
//                    httpResDescList.add("// " + comment + HttpUtils.CR_LF);
//
//                    String evalJsRes = jsExecutor.evalJsAfterRequest(
//                        jsAfterReq,
//                        new org.javamaster.httpclient.utils.Triple<>(SimpleTypeEnum.JSON, byteArray, ContentType.APPLICATION_JSON.getMimeType()),
//                        200,
//                        new java.util.HashMap<>()
//                    );
//
//                    if (evalJsRes != null && !evalJsRes.isEmpty()) {
//                        httpResDescList.add("/*" + HttpUtils.CR_LF + NlsBundle.message("post.js.executed.result") + ":" + HttpUtils.CR_LF);
//                        httpResDescList.add(evalJsRes + HttpUtils.CR_LF);
//                        httpResDescList.add("*/" + HttpUtils.CR_LF);
//                    }
//
//                    httpResDescList.add("### " + tabName + HttpUtils.CR_LF);
//                    httpResDescList.add("DUBBO " + url + " " + HttpUtils.CR_LF);
//                    httpResDescList.add(HttpHeaders.CONTENT_LENGTH + ": " + byteArray.length + HttpUtils.CR_LF);
//
//                    reqHeaderMap.forEach((name, values) -> {
//                        values.forEach(value -> {
//                            httpResDescList.add(name + ": " + value + HttpUtils.CR_LF);
//                        });
//                    });
//                    httpResDescList.add(HttpUtils.CR_LF);
//
//                    httpResDescList.add(new String(byteArray, StandardCharsets.UTF_8));
//
//                    HttpInfo httpInfo = new HttpInfo(
//                        httpReqDescList,
//                        httpResDescList,
//                        SimpleTypeEnum.JSON,
//                        byteArray,
//                        null,
//                        ContentType.APPLICATION_JSON.getMimeType()
//                    );
//
//                    dealResponse(httpInfo, parentPath);
//                });
//            });
//
//            destroyProcess();
//        });
//
//        cancelFutureIfTerminated(future);
//    }

    private void handleHttp(
        Project project,
        String url,
        LinkedMultiValueMap<String, String> reqHeaderMap,
        Object reqBody,
        List<String> httpReqDescList
    ) {
        long start = System.currentTimeMillis();

        CompletableFuture<HttpResponse> future =
            methodType.execute(project, url, version, reqHeaderMap, reqBody, httpReqDescList, tabName, paramMap);

        future.whenCompleteAsync((response, throwable) -> {
            Application.get().invokeLater(() -> {
                Application.get().runWriteAction(() -> {
                    try {
                        httpStatus = response != null ? response.statusCode() : null;
                        costTimes = System.currentTimeMillis() - start;

                        if (throwable != null) {
                            HttpInfo httpInfo = new HttpInfo(httpReqDescList, new java.util.ArrayList<>(), null, null, throwable);
                            dealResponse(httpInfo, parentPath);
                            return;
                        }

                        String size = StringUtil.formatFileSize(response.body().length);

                        List<String> resHeaderList = HttpUtils.convertToResHeaderDescList(response);

                        Trinity<SimpleTypeEnum, byte[], String> resTriple = HttpUtils.convertToResPair(response);

                        String comment = HttpClientLocalize.resDesc(response.statusCode(), costTimes, size).get();

                        java.util.List<String> httpResDescList = new java.util.ArrayList<>();
                        httpResDescList.add("// " + comment + HttpUtils.CR_LF);

                        String evalJsRes = jsExecutor.evalJsAfterRequest(
                            jsAfterReq,
                            resTriple,
                            response.statusCode(),
                            response.headers()
                        );

                        if (evalJsRes != null && !evalJsRes.isEmpty()) {
                            httpResDescList.add("/*" + HttpUtils.CR_LF + HttpClientLocalize.postJsExecutedResult().get() + ":" + HttpUtils.CR_LF);
                            httpResDescList.add(evalJsRes + HttpUtils.CR_LF);
                            httpResDescList.add("*/" + HttpUtils.CR_LF);
                        }

                        String versionDesc = HttpUtils.getVersionDesc(response.version());

                        String commentTabName = "### " + tabName + HttpUtils.CR_LF;
                        httpResDescList.add(commentTabName);

                        httpResDescList.add(methodType.name() + " " + response.uri() + " " + versionDesc + HttpUtils.CR_LF);

                        httpResDescList.addAll(resHeaderList);

                        if (resTriple.getFirst().isBinary()) {
                            httpResDescList.add(NlsBundle.message("res.binary.data", size));
                        }
                        else {
                            httpResDescList.add(new String(resTriple.getSecond(), StandardCharsets.UTF_8));
                        }

                        HttpInfo httpInfo = new HttpInfo(
                            httpReqDescList, httpResDescList, resTriple.getFirst(), resTriple.getSecond(),
                            null, resTriple.getThird()
                        );

                        dealResponse(httpInfo, parentPath);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        NotifyUtil.notifyError(this.project, e.toString());
                    }
                });
            });

            destroyProcess();
        });

        cancelFutureIfTerminated(future);
    }

    @RequiredReadAction
    private void dealResponse(HttpInfo httpInfo, String parentPath) {
        HttpRequestTarget requestTargetLocal = PsiTreeUtil.getNextSiblingOfType(httpMethod, HttpRequestTarget.class);

        HttpRequest httpRequest = PsiTreeUtil.getParentOfType(requestTargetLocal, HttpRequest.class);

        String outPutFilePath = null;
        HttpOutputFile httpOutputFile = PsiTreeUtil.getChildOfType(httpRequest, HttpOutputFile.class);
        if (httpOutputFile != null) {
            HttpFilePath filePath = httpOutputFile.getFilePath();
            if (filePath != null) {
                outPutFilePath = filePath.getText();
            }
        }

        String saveResult = saveResToFile(outPutFilePath, parentPath, httpInfo.getByteArray());
        if (saveResult != null) {
            httpInfo.getHttpResDescList().add(0, saveResult);
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.SERVICES);

        if (toolWindow != null) {
            Content content = toolWindow.getContentManager().getContent(getComponent());
            if (content != null) {
                if (httpDashboardForm == null) {
                    httpDashboardForm = new HttpDashboardForm(tabName, project);
                }
                content.setDisposer(httpDashboardForm);
            }
            else {
                if (httpDashboardForm == null) {
                    httpDashboardForm = new HttpDashboardForm(tabName, project);
                }
                Disposer.register(Disposable.newDisposable(), httpDashboardForm);
            }
        }

        if (httpDashboardForm == null) {
            httpDashboardForm = new HttpDashboardForm(tabName, project);
        }
        httpDashboardForm.initHttpResContent(httpInfo, paramMap.containsKey(ParamEnum.NO_LOG.getParam()));

        Throwable myThrowable = httpDashboardForm.getThrowable();
        hasError = myThrowable != null;
        if (hasError) {
            myThrowable.printStackTrace();

            LocalizeValue error = (myThrowable instanceof CancellationException || myThrowable.getCause() instanceof CancellationException)
                ? HttpClientLocalize.reqInterrupted(tabName)
                : HttpClientLocalize.reqFailed(tabName, myThrowable);
            String msg = "<div style='font-size:13pt'>" + error + "</div>";
            toolWindowManager.notifyByBalloon(ToolWindowId.SERVICES, NotificationType.ERROR, msg);
        }
        else {
            String msg = "<div style='font-size:13pt'>" + tabName + " " + HttpClientLocalize.requestSuccess() + "!</div>";
            toolWindowManager.notifyByBalloon(ToolWindowId.SERVICES, NotificationType.INFO, msg);
        }
    }

    private String saveResToFile(String outPutFilePath, String parentPath, byte[] byteArray) {
        if (outPutFilePath == null) {
            return null;
        }

        if (byteArray == null) {
            return null;
        }

        String path = variableResolver.resolve(outPutFilePath);

        path = HttpUtils.constructFilePath(path, parentPath);

        File file = new File(path);

        if (!file.getParentFile().exists()) {
            try {
                Files.createDirectories(file.toPath());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray)) {
                Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return "// " + HttpClientLocalize.saveFailed().get() + ": " + e + HttpUtils.CR_LF;
        }

        VirtualFileManager.getInstance().asyncRefresh(null);

        return "// " + HttpClientLocalize.saveToFile() + ": " + file.toPath().normalize().toAbsolutePath() + HttpUtils.CR_LF;
    }

    private void cancelFutureIfTerminated(CompletableFuture<?> future) {
        // TODO
//        CompletableFuture.runAsync(() -> {
//            while (!isProcessTerminated() && !RunFileHandler.isInterrupted()) {
//                try {
//                    Thread.sleep(600);
//                }
//                catch (InterruptedException e) {
//                    break;
//                }
//            }
//
//            if (loadingRemover != null) {
//                Application.get().invokeLater(() -> {
//                    loadingRemover.run();
//                });
//            }
//
//            future.cancel(true);
//        });
    }

    @Override
    protected void destroyProcessImpl() {
        if (loadingRemover != null) {
            Application.get().invokeLater(() -> loadingRemover.run());
        }

        /// TODO
//        if (wsRequest != null) {
//            wsRequest.abortConnect();
//        }

        // TODO
//        if (serverSocket != null) {
//            try {
//                serverSocket.close();
//            }
//            catch (Exception e) {
//                // ignore
//            }
//        }

        int code = hasError ? HttpUtils.FAILED : HttpUtils.SUCCESS;

        httpMethod.putUserData(HttpUtils.requestFinishedKey, code);

       // TODO RunFileHandler.resetInterrupt();

        notifyProcessTerminated(code);
    }

    @Override
    protected void detachProcessImpl() {
        destroyProcessImpl();
        notifyProcessDetached();
    }

    @Override
    public boolean detachIsDefault() {
        return true;
    }

    @Nullable
    @Override
    public OutputStream getProcessInput() {
        return null;
    }
}
