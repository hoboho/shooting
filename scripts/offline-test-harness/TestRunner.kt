import org.junit.Test
import java.lang.reflect.InvocationTargetException

/**
 * Tiny deterministic runner used to execute the repository's JUnit-style JVM
 * tests in an offline sandbox where Gradle/Maven are unavailable. It discovers
 * @Test methods via reflection and reports every failure.
 */
fun main(args: Array<String>) {
    var passed = 0
    var failed = 0
    val failures = ArrayList<String>()
    for (className in args) {
        val clazz = Class.forName(className)
        val instance = clazz.getDeclaredConstructor().newInstance()
        val methods = clazz.declaredMethods
            .filter { it.isAnnotationPresent(Test::class.java) }
            .sortedBy { it.name }
        for (method in methods) {
            try {
                method.isAccessible = true
                method.invoke(instance)
                passed++
                println("PASS  ${clazz.simpleName}.${method.name}")
            } catch (e: InvocationTargetException) {
                failed++
                val cause = e.targetException
                failures += "${clazz.simpleName}.${method.name}: ${cause.javaClass.simpleName}: ${cause.message}"
                println("FAIL  ${clazz.simpleName}.${method.name}: ${cause.javaClass.simpleName}: ${cause.message}")
            } catch (e: Throwable) {
                failed++
                failures += "${clazz.simpleName}.${method.name}: $e"
                println("FAIL  ${clazz.simpleName}.${method.name}: $e")
            }
        }
    }
    println("\n===== JVM test summary: $passed passed, $failed failed =====")
    if (failures.isNotEmpty()) {
        println("Failures:\n - " + failures.joinToString("\n - "))
        kotlin.system.exitProcess(1)
    }
}
