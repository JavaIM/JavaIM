package org.yuezhikong

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jline.jansi.AnsiConsole
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.yuezhikong.Main.ConsoleMain
import org.yuezhikong.Server.ServerTools
import org.yuezhikong.utils.ConfigFileManager
import org.yuezhikong.utils.ConsoleCommandRequest
import org.yuezhikong.utils.checkUpdate.CheckUpdate
import java.io.File
import java.io.IOException
import java.security.Security
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

var log: org.slf4j.Logger ?= null
var terminal: Terminal ?= null

fun main(args: Array<String>) = application {
    init()
    // 初始化 LineReader
    val reader = LineReaderBuilder.builder().terminal(terminal).build()
    // 命令行参数处理
    val commandLineArgs = ConsoleCommandRequest.commandLineRequest(args)

    if (commandLineArgs.containsKey("nogui")) {
        log?.info("进入无界面模式")
        ConsoleMain(commandLineArgs, reader)
    }
    else{
        Window(onCloseRequest = ::exitApplication, title = "JavaIM") {
            JavaIMTheme {
                App(args)
            }
        }
    }
}

@Composable
fun JavaIMTheme(content: @Composable () -> Unit) {
    val md3Blue = Color(0xFF6366F1)
    val md3Purple = Color(0xFF8B5CF6)
    val md3DarkBg = Color(0xFF0F172A)
    val md3Surface = Color(0xFF1E293B)
    val md3Error = Color(0xFFFF6B6B)

    MaterialTheme(
        colors = MaterialTheme.colors.copy(
            primary = md3Blue,
            secondary = md3Purple,
            error = md3Error,
            background = md3DarkBg,
            surface = md3Surface
        ),
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}

@Composable
fun App(args: Array<String>) {
    // 设定默认的未捕获异常处理器
    Thread.setDefaultUncaughtExceptionHandler(CrashReport.getCrashReport())
    // 初始化Shutdown Hook
    Runtime.getRuntime().addShutdownHook(Thread(Runnable {
        try {
            if (ServerTools.getServerInstance() == null || !ServerTools.getServerInstance()
                    .isServerCompleteStart()
            ) return@Runnable
            try {
                ServerTools.getServerInstance().stop()
            } catch (ignored: IllegalStateException) {
            }
        } catch (ignored: Throwable) {
        }
    }))
    // 初始化BouncyCastle，设置为JCE Provider
    Security.addProvider(BouncyCastleProvider())
    // 初始化主线程崩溃报告程序
    Thread.currentThread().setUncaughtExceptionHandler(CrashReport.getCrashReport())

    if (!(File("server.properties").exists())) {
        log?.info("目录下没有检测到服务端配置文件，判断为第一次进入")
        var isVisible by remember { mutableStateOf(true) }
        ConfigFileManager.createServerConfig()
        if (isVisible){ onFirstStart(onClose = { isVisible = false }) }
    } else
        ConfigFileManager.reloadServerConfig()

    // 检查自动更新设置
    val checkUpdateSetting = ConfigFileManager.getServerConfig("checkUpdate").toBoolean()
    if (checkUpdateSetting) {
        checkUpdateUI()
    }
}

@Composable
fun checkUpdateUI() {
    val showUpdateDialog = mutableStateOf(true)
    val installUpdate = mutableStateOf(false)
    val githubAccessToken = mutableStateOf(ConfigFileManager.getServerConfig("githubAccessToken") ?: "")
    val showTokenInput = mutableStateOf(githubAccessToken.value.isEmpty())
    var tempToken by remember {mutableStateOf("")}

    if (showUpdateDialog.value) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showTokenInput.value) {
                // 询问是否自动安装更新
                Text(
                    "是否自动安装更新？",
                    style = MaterialTheme.typography.h5
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row {
                    Button(
                        onClick = {
                            installUpdate.value = true
                            CheckUpdate.checkUpdate(true, githubAccessToken.value)
                            showUpdateDialog.value = false
                        },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("是")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            installUpdate.value = false
                            CheckUpdate.checkUpdate(false, githubAccessToken.value)
                            showUpdateDialog.value = false
                        },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("否")
                    }
                }
            } else {
                // GitHub Token 输入界面
                            Text(
                                "检测到您未设置Github Token",
                                style = MaterialTheme.typography.h5
                            )
                Spacer(modifier = Modifier.height(16.dp))
                Text("是否设置Github Token以启用自动更新？")
                Spacer(modifier = Modifier.height(32.dp))

                TextField(
                    value = tempToken,
                    onValueChange = { tempToken = it },
                    label = { Text("Github Token") },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                Row {
                    Button(
                        onClick = {
                            if (tempToken.isNotEmpty()) {
                                githubAccessToken.value = tempToken
                                ConfigFileManager.setServerConfig("githubAccessToken", tempToken)
                                ConfigFileManager.rewriteServerConfig()
                                showTokenInput.value = false
                            }
                        },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("保存")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            log?.info("未设置Github Token, 无法进行自动更新，已跳过更新任务")
                            showUpdateDialog.value = false
                        },
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("跳过")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun onFirstStart(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()

    // 配置状态
    val shouldConfigure = mutableStateOf(false)
    val serverName = mutableStateOf("")
    val checkUpdate = mutableStateOf(false)
    val useSqlite = mutableStateOf(true)
    val mysqlHost = mutableStateOf("")
    val mysqlPort = mutableStateOf("")
    val mysqlDBName = mutableStateOf("")
    val mysqlUser = mutableStateOf("")
    val mysqlPasswd = mutableStateOf("")

    Column(
        Modifier.fillMaxSize(),
    ) {
        val pagerState = rememberPagerState(pageCount = { 8 })
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
        ) { pages ->
            when (pages) {
                // 页面0：欢迎界面
                0 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "欢迎来到JavaIM！",
                                style = MaterialTheme.typography.h4
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("这是首次启动向导，将帮助您快速配置JavaIM服务器。")
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(1) }
                                },
                                modifier = Modifier.width(200.dp)
                            ) {
                                Text("开始配置")
                            }
                        }
                    }
                }
                // 页面1：询问是否进行配置
                1 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "是否进行配置？",
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row {
                                Button(
                                    onClick = {
                                        shouldConfigure.value = true
                                        scope.launch { pagerState.animateScrollToPage(2) }
                                    },
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    Text("是")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        shouldConfigure.value = false
                                        scope.launch { pagerState.animateScrollToPage(7) }
                                    },
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    Text("否")
                                }
                            }
                        }
                    }
                }
                // 页面2：设置服务器名称
                2 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp)
                                .width(400.dp)
                        ) {
                            Text(
                                "设置服务器名称",
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextField(
                                value = serverName.value,
                                onValueChange = { serverName.value = it },
                                label = { Text("服务器名称") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row {
                                Button(
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("上一步")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        if (serverName.value.isNotEmpty()) {
                                            scope.launch { pagerState.animateScrollToPage(3) }
                                        }
                                    },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("下一步")
                                }
                            }
                        }
                    }
                }
                // 页面3：询问是否自动检查更新
                3 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "是否自动检查更新？",
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row {
                                Button(
                                    onClick = {
                                        checkUpdate.value = true
                                        scope.launch { pagerState.animateScrollToPage(4) }
                                    },
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    Text("是")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        checkUpdate.value = false
                                        scope.launch { pagerState.animateScrollToPage(4) }
                                    },
                                    modifier = Modifier.width(120.dp)
                                ) {
                                    Text("否")
                                }
                            }
                        }
                    }
                }
                // 页面4：选择数据库类型
                4 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "选择数据库",
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                RadioButton(
                                    selected = useSqlite.value,
                                    onClick = { useSqlite.value = true }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("使用SQLite（推荐）")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                RadioButton(
                                    selected = !useSqlite.value,
                                    onClick = { useSqlite.value = false }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("使用MySQL")
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Row {
                                Button(
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(3) }
                                    },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("上一步")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        if (useSqlite.value) {
                                            scope.launch { pagerState.animateScrollToPage(6) }
                                        } else {
                                            scope.launch { pagerState.animateScrollToPage(5) }
                                        }
                                    },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("下一步")
                                }
                            }
                        }
                    }
                }
                // 页面5：设置MySQL配置
                5 -> {
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.width(400.dp)
                        ) {
                            Text(
                                "设置MySQL数据库",
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextField(
                                value = mysqlHost.value,
                                onValueChange = { mysqlHost.value = it },
                                label = { Text("MySQL地址") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = mysqlPort.value,
                                onValueChange = { mysqlPort.value = it },
                                label = { Text("MySQL端口") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = mysqlDBName.value,
                                onValueChange = { mysqlDBName.value = it },
                                label = { Text("数据库名称") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = mysqlUser.value,
                                onValueChange = { mysqlUser.value = it },
                                label = { Text("登录用户") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = mysqlPasswd.value,
                                onValueChange = { mysqlPasswd.value = it },
                                label = { Text("登录密码") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row {
                                Button(
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(4) }
                                    },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("上一步")
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        if (mysqlHost.value.isNotEmpty() && mysqlPort.value.isNotEmpty() &&
                                            mysqlDBName.value.isNotEmpty() && mysqlUser.value.isNotEmpty()
                                        ) {
                                            scope.launch { pagerState.animateScrollToPage(6) }
                                        }
                                    },
                                    modifier = Modifier.width(100.dp)
                                ) {
                                    Text("下一步")
                                }
                            }
                        }
                    }
                }
                // 页面6：检查和保存配置
                6 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "您的配置已经保存",
                                style = MaterialTheme.typography.h5
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            // 保存配置的逻辑
                            LaunchedEffect(Unit) {
                                try {
                                    ConfigFileManager.setServerConfig("serverName", serverName.value)
                                    ConfigFileManager.setServerConfig("checkUpdate", checkUpdate.value.toString())

                                    if (useSqlite.value) {
                                        ConfigFileManager.setServerConfig("sqlite", "true")
                                        ConfigFileManager.setServerConfig("mysqlHost", "")
                                        ConfigFileManager.setServerConfig("mysqlPort", "")
                                        ConfigFileManager.setServerConfig("mysqlDBName", "")
                                        ConfigFileManager.setServerConfig("mysqlUser", "")
                                        ConfigFileManager.setServerConfig("mysqlPasswd", "")
                                    } else {
                                        ConfigFileManager.setServerConfig("sqlite", "false")
                                        ConfigFileManager.setServerConfig("mysqlHost", mysqlHost.value)
                                        ConfigFileManager.setServerConfig("mysqlPort", mysqlPort.value)
                                        ConfigFileManager.setServerConfig("mysqlDBName", mysqlDBName.value)
                                        ConfigFileManager.setServerConfig("mysqlUser", mysqlUser.value)
                                        ConfigFileManager.setServerConfig("mysqlPasswd", mysqlPasswd.value)
                                    }

                                    ConfigFileManager.rewriteServerConfig()
                                    log?.info("配置已保存")
                                } catch (e: Exception) {
                                    log?.error("保存配置失败: ${e.message}")
                                }
                            }

                            Button(
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(7) }
                                },
                                modifier = Modifier.width(200.dp)
                            ) {
                                Text("继续")
                            }
                        }
                    }
                }
                // 页面7：完成界面
                7 -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                "设置向导完成！",
                                style = MaterialTheme.typography.h4
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("服务器已配置完成，您可以随时修改配置文件来调整设置。")
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                "服务器名称: ${serverName.value}",
                                style = MaterialTheme.typography.body1
                            )
                            Text(
                                "自动检查更新: ${if (checkUpdate.value) "是" else "否"}",
                                style = MaterialTheme.typography.body1
                            )
                            Text(
                                "数据库: ${if (useSqlite.value) "SQLite" else "MySQL"}",
                                style = MaterialTheme.typography.body1
                            )
                            Button(
                                onClick = { onClose() }
                            ){
                                Text("完成")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun init() {
    log = LoggerFactory.getLogger(Main::class.java)
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()
    log?.let{
        if (it.isTraceEnabled()) Logger.getLogger("").setLevel(Level.FINEST)

        it.info("正在初始化Jline...")
        var terminal1: Terminal?
        try {
            if (System.console() != null) {
                AnsiConsole.systemInstall()
                terminal1 = AnsiConsole.getTerminal()
            } else terminal1 =
                TerminalBuilder.builder().system(true).exec(false).ffm(false).jna(false).dumb(true).build()
        } catch (e: IOException) {
            it.error("JavaIM 初始化失败")
            exitProcess(1)
        }
        terminal = terminal1
        it.info("JavaIM初始化完成")
    }
}
