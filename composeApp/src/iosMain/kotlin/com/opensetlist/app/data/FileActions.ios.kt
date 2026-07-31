package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeImport
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

private class FilePickerDelegate(
    private val onPicked: (String) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: return
        val path = url.path ?: return
        val content = readFile(path)
        if (content != null) onPicked(content)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {}
}

private object PickerDelegateHolder {
    var importDelegate: FilePickerDelegate? = null
}

private fun readFile(path: String): String? = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, error.ptr)
}

private fun writeTempFile(fileName: String, content: String): String? = memScoped {
    val dir = NSTemporaryDirectory()
    val path = "$dir$fileName"
    val error = alloc<ObjCObjectVar<NSError?>>()
    val ok = NSString.create(string = content)
        .writeToFile(path, true, NSUTF8StringEncoding, error.ptr)
    if (ok) path else null
}

private fun presentViewController(viewController: UIViewController) {
    UIApplication.sharedApplication.keyWindow
        ?.rootViewController
        ?.presentViewController(viewController, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun writeTempFileBytes(fileName: String, bytes: ByteArray): String? {
    val dir = NSTemporaryDirectory()
    val path = "$dir$fileName"
    val data = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }
    return if (data.writeToFile(path, true)) path else null
}

@Composable
actual fun rememberFileActions(
    getExportContent: () -> String?,
    onImported: (String) -> Unit,
    onExported: (Boolean) -> Unit,
    onShared: (Boolean) -> Unit,
    getExportBytes: () -> ByteArray?
): FileActions {
    val currentExportContent = rememberUpdatedState(getExportContent)
    val currentExportBytes = rememberUpdatedState(getExportBytes)

    fun presentActivity(fileName: String, completed: (Boolean) -> Unit) {
        val bytes = currentExportBytes.value()
        val path = when {
            bytes != null -> writeTempFileBytes(fileName, bytes)
            else -> {
                val content = currentExportContent.value() ?: run {
                    completed(false)
                    return
                }
                writeTempFile(fileName, content)
            }
        }
        if (path == null) {
            completed(false)
            return
        }
        val url = NSURL.fileURLWithPath(path)
        val activityVC = UIActivityViewController(
            activityItems = listOf(url),
            applicationActivities = null
        )
        activityVC.completionWithItemsHandler = { _, didComplete, _, _ ->
            completed(didComplete)
        }
        presentViewController(activityVC)
    }

    return remember {
        FileActions(
            importFile = {
                val delegate = FilePickerDelegate { content -> onImported(content) }
                PickerDelegateHolder.importDelegate = delegate
                val picker = UIDocumentPickerViewController(
                    documentTypes = listOf("public.text", "public.data", "public.json"),
                    inMode = UIDocumentPickerModeImport
                )
                picker.delegate = delegate
                presentViewController(picker)
            },
            saveFile = { fileName, _ ->
                presentActivity(fileName, onExported)
            },
            shareFile = { fileName, _ ->
                presentActivity(fileName, onShared)
            },
            openUrl = { url ->
                NSURL.URLWithString(url)?.let {
                    UIApplication.sharedApplication.openURL(it)
                }
            }
        )
    }
}
