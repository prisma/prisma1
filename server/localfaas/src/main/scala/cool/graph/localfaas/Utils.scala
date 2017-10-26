package cool.graph.localfaas

import java.io.FileInputStream

import better.files.File
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveStreamFactory}
import org.apache.commons.compress.utils.IOUtils

import scala.util.{Failure, Try}

object Utils {
  def unzip(source: File, target: File): Unit = {
    val inputStream   = new FileInputStream(source.path.toFile)
    val archiveStream = new ArchiveStreamFactory().createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream)

    def stream: Stream[ArchiveEntry] = archiveStream.getNextEntry match {
      case null  => Stream.empty
      case entry => entry #:: stream
    }

    def closeStreams = {
      archiveStream.close()
      inputStream.close()
    }

    Try {
      for (entry <- stream if !entry.isDirectory) {
        val outFile = (target / entry.getName).createIfNotExists(asDirectory = false, createParents = true).clear()
        val os      = outFile.newOutputStream

        Try { IOUtils.copy(archiveStream, os) } match {
          case Failure(e) => os.close(); throw e
          case _          => os.close()
        }
      }
    } match {
      case Failure(e) => closeStreams; throw e
      case _          => closeStreams
    }
  }
}
