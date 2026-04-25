package flotti

fun main(args: Array<String>) {
    loadEnv()
    io.ktor.server.netty.EngineMain.main(args)
}
