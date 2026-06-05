package org.yuezhikong

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jline.jansi.AnsiConsole
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.yuezhikong.Server.ServerTools
import org.yuezhikong.utils.ConfigFileManager
import org.yuezhikong.utils.Notice
import java.io.File
import java.io.IOException
import java.security.Security
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

var log: org.slf4j.Logger ?= null
var terminal: Terminal ?= null

fun main() = application {
    init()
    Window(onCloseRequest = ::exitApplication, title = "JavaIM") {
        MaterialTheme {
            App()
        }
    }
}

@Composable
fun App() {
    val reader = LineReaderBuilder.builder().terminal(terminal).build()

    if (!(File("server.properties").exists())) {
        log?.info("目录下没有检测到服务端配置文件，判断为第一次进入")
        ConfigFileManager.createServerConfig()
        onFirstStart(reader)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun onFirstStart(reader: LineReader) {
    val scope = rememberCoroutineScope()
    Column (
        Modifier.fillMaxSize(),
    ){
        val pagerState = rememberPagerState(pageCount = { 5 })
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
        ) { pages ->
                when(pages) {
                    0 -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column {
                                Text("欢迎来到JavaIM！")
                                Button(
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                ) {
                                    Text("下一步")
                                }
                            }
                        }
                    }
                    1 -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text("正在引导您完成第一次启动的配置...")
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
            terminal1 = null
            it.error("JavaIM 初始化失败")
            exitProcess(1)
        }
        terminal = terminal1
        it.info("JavaIM初始化完成")
    }
}

fun onStart() {
    Thread.setDefaultUncaughtExceptionHandler(CrashReport.getCrashReport())
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

    Security.addProvider(BouncyCastleProvider())

    Thread.currentThread().setUncaughtExceptionHandler(CrashReport.getCrashReport())

}
