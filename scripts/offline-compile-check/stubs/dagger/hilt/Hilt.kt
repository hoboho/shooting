package dagger.hilt
import kotlin.reflect.KClass
annotation class InstallIn(vararg val value: KClass<*>)
