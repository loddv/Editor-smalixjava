package com.smalixjava.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import io.github.rosemoe.sora.widget.CodeEditor

class MainActivity : Activity() {
    private lateinit var directoryLabel: TextView
    private lateinit var fileList: LinearLayout
    private lateinit var fileListScroll: ScrollView
    private lateinit var editorContainer: LinearLayout
    private lateinit var editorTitle: TextView
    private lateinit var codeEditor: CodeEditor

    private val directoryStack = ArrayDeque<DocumentFile>()
    private var openedFile: DocumentFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createLayout()
    }

    private fun createLayout() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        actions.addView(
            actionButton("Abrir pasta Smali") { chooseSmaliFolder() },
            weightedParams()
        )
        actions.addView(
            actionButton("Abrir APK") { chooseApk() },
            weightedParams()
        )
        content.addView(actions)

        directoryLabel = TextView(this).apply {
            text = "Abra uma pasta /smali ou selecione um APK"
            textSize = 16f
            setPadding(0, dp(12), 0, dp(8))
        }
        content.addView(directoryLabel)

        fileList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        fileListScroll = ScrollView(this).apply {
            addView(fileList)
        }
        content.addView(
            fileListScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        editorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        editorTitle = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(8), 0, dp(4))
        }
        editorContainer.addView(editorTitle)

        codeEditor = CodeEditor(this)
        editorContainer.addView(
            codeEditor,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        editorContainer.addView(actionButton("Salvar alterações") { saveOpenedFile() })
        content.addView(
            editorContainer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(content)
    }

    private fun chooseSmaliFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        startActivityForResult(intent, REQUEST_SMALI_FOLDER)
    }

    private fun chooseApk() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/vnd.android.package-archive", "application/octet-stream")
            )
        }
        startActivityForResult(intent, REQUEST_APK)
    }

    @Deprecated("Uses the platform document picker without requiring additional UI dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        when (requestCode) {
            REQUEST_SMALI_FOLDER -> openSmaliFolder(uri, data.flags)
            REQUEST_APK -> showApkSelected(uri)
        }
    }

    private fun openSmaliFolder(uri: Uri, flags: Int) {
        val permissionFlags = flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching {
            contentResolver.takePersistableUriPermission(uri, permissionFlags)
        }.onFailure {
            showMessage("Não foi possível manter o acesso à pasta selecionada.")
        }

        val root = DocumentFile.fromTreeUri(this, uri)
        if (root == null || !root.isDirectory || !root.canRead()) {
            showMessage("Selecione uma pasta acessível.")
            return
        }

        directoryStack.clear()
        directoryStack.addLast(root)
        openedFile = null
        editorContainer.visibility = View.GONE
        renderDirectory()
    }

    private fun showApkSelected(uri: Uri) {
        val name = DocumentFile.fromSingleUri(this, uri)?.name ?: "APK selecionado"
        directoryLabel.text =
            "$name — a decodificação e a montagem do APK serão adicionadas em uma próxima etapa."
        showMessage("APK selecionado. A decodificação ainda não está disponível.")
    }

    private fun renderDirectory() {
        val directory = directoryStack.lastOrNull() ?: return
        directoryLabel.text = directoryStack.joinToString(" / ") { it.name ?: "smali" }
        fileList.removeAllViews()

        if (directoryStack.size > 1) {
            fileList.addView(actionButton("↑ Pasta anterior") {
                directoryStack.removeLast()
                renderDirectory()
            })
        }

        val entries = runCatching { directory.listFiles().toList() }
            .getOrElse {
                showMessage("Não foi possível listar esta pasta.")
                emptyList()
            }
            .filter { file ->
                file.isDirectory || (file.isFile && file.name?.endsWith(".smali", true) == true)
            }
            .sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase() })

        if (entries.isEmpty()) {
            fileList.addView(TextView(this).apply {
                text = "Nenhuma subpasta ou arquivo .smali nesta pasta."
                setPadding(0, dp(12), 0, dp(12))
            })
        }

        entries.forEach { entry ->
            val label = if (entry.isDirectory) "📁 ${entry.name}/" else "  ${entry.name}"
            fileList.addView(actionButton(label) {
                if (entry.isDirectory) {
                    directoryStack.addLast(entry)
                    renderDirectory()
                } else {
                    openSmaliFile(entry)
                }
            })
        }
    }

    private fun openSmaliFile(file: DocumentFile) {
        val text = runCatching {
            contentResolver.openInputStream(file.uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: error("Não foi possível abrir o arquivo.")
        }.getOrElse {
            showMessage("Falha ao abrir ${file.name}.")
            return
        }

        openedFile = file
        editorTitle.text = file.name ?: "Arquivo Smali"
        codeEditor.setText(text)
        editorContainer.visibility = View.VISIBLE
    }

    private fun saveOpenedFile() {
        val file = openedFile ?: return
        runCatching {
            contentResolver.openOutputStream(file.uri, "wt")
                ?.bufferedWriter(Charsets.UTF_8)
                ?.use { it.write(codeEditor.text.toString()) }
                ?: error("Não foi possível gravar o arquivo.")
        }.onSuccess {
            showMessage("Alterações salvas.")
        }.onFailure {
            showMessage("Falha ao salvar ${file.name}.")
        }
    }

    override fun onBackPressed() {
        when {
            editorContainer.visibility == View.VISIBLE -> {
                editorContainer.visibility = View.GONE
                openedFile = null
            }
            directoryStack.size > 1 -> {
                directoryStack.removeLast()
                renderDirectory()
            }
            else -> super.onBackPressed()
        }
    }

    private fun actionButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener { action() }
        }

    private fun weightedParams() =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun showMessage(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val REQUEST_SMALI_FOLDER = 1
        private const val REQUEST_APK = 2
    }
}
