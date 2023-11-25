package dev.defvs.asxresolver

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

fun main(args: Array<String>) {
	if (args.isEmpty()) {
		println("No directory path provided. Please specify the path as a command-line argument.")
		return
	}

	val directoryPath = args[0] // Use the first command-line argument as the directory path
	findAndCopyAsxFiles(directoryPath)
}

fun findAndCopyAsxFiles(directoryPath: String) {
	val directory = File(directoryPath)
	val asxFiles = directory.walk().filter { it.extension == "asx" }

	asxFiles.forEach { asxFile ->
		val (refPath, tags) = extractRefPathAndTags(asxFile)
		refPath?.let {
			val sourceFile = File(it)
			if (!sourceFile.exists()) {
				println("Error: Source file does not exist: ${sourceFile.path}")
				return@forEach
			}

			val newFileName = "${asxFile.nameWithoutExtension}.${sourceFile.extension}"
			val targetFile = File(asxFile.parent, newFileName)

			try {
				Files.copy(sourceFile.toPath(), targetFile.toPath())
				updateTags(targetFile, tags)
			} catch (e: FileAlreadyExistsException) {
				println("File already exists and will not be copied: ${targetFile.path}")
			} catch (e: Exception) {
				println("Error occurred while copying file: ${e.message}")
			}
		}
	}
}

fun extractRefPathAndTags(asxFile: File): Pair<String?, Map<FieldKey, String>> {
	val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
	val doc = documentBuilder.parse(asxFile)
	val refNodes = doc.getElementsByTagName("ref")
	val tags = mutableMapOf<FieldKey, String>()

	// Extract tag information
	val params = doc.getElementsByTagName("param")
	for (i in 0..<params.length) {
		val param = params.item(i).attributes
		val name = param.getNamedItem("name")?.nodeValue
		val value = param.getNamedItem("value")?.nodeValue

		when (name) {
			"WM/AlbumTitle" -> tags[FieldKey.ALBUM] = value ?: ""
			"WM/AlbumArtist" -> tags[FieldKey.ALBUM_ARTIST] = value ?: ""
			"WM/Year" -> tags[FieldKey.YEAR] = value ?: ""
			"WM/TrackNumber" -> tags[FieldKey.TRACK] = value?.split("/")?.get(0) ?: ""
		}
	}

	// Extract title and author
	val entry = doc.getElementsByTagName("entry").item(0)
	val title = entry?.childNodes?.let { nodes ->
		for (i in 0..<nodes.length) {
			val node = nodes.item(i)
			if (node.nodeName == "title") return@let node.textContent
		}
		null
	}
	val author = entry?.childNodes?.let { nodes ->
		for (i in 0..<nodes.length) {
			val node = nodes.item(i)
			if (node.nodeName == "author") return@let node.textContent
		}
		null
	}

	title?.let { tags[FieldKey.TITLE] = it }
	author?.let { tags[FieldKey.ARTIST] = it }

	val href = if (refNodes.length > 0) refNodes.item(0).attributes.getNamedItem("href")?.nodeValue else null
	return Pair(href, tags)
}

fun updateTags(audioFile: File, tags: Map<FieldKey, String>) {
	try {
		val file = AudioFileIO.read(audioFile)
		val tag = file.tag

		tags.forEach { (key, value) ->
			if (value.isNotBlank()) {
				tag.setField(key, value)
			}
		}

		file.commit()
	} catch (e: Exception) {
		e.printStackTrace()
	}
}

// Add additional error handling and checks as needed.