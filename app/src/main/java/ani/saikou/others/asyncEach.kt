package ani.saikou.others

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun <K, V, R> Map<out K, V>.asyncEach(f: suspend (Map.Entry<K, V>) -> R): List<R> = runBlocking {
    map { withContext(Dispatchers.IO) { async { f(it) } } }.map { it.await() }
}

fun <A, B> Collection<A>.asyncEach(f: suspend (A) -> B): List<B> = runBlocking {
    map { async { f(it) } }.map { it.await() }
}