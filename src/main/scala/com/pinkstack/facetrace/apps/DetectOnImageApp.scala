package com.pinkstack.facetrace.apps

import cats.effect.{IO, Resource}
import cats.effect.*
import cats.implicits.*
import cats.data.NonEmptyList
import com.monovore.decline.*
import com.monovore.decline.effect.*
import org.bytedeco.javacv.*
import org.bytedeco.javacpp.*
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.global.opencv_core.*
import org.bytedeco.opencv.global.opencv_imgproc.*
import org.bytedeco.opencv.global.opencv_imgcodecs.*
import org.bytedeco.opencv.opencv_core.{Mat, Point, Rect, RectVector, Scalar}
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j._

import java.nio.file.{Path, Paths}

given Conversion[Path, String]            = _.toAbsolutePath.toString
given Conversion[RectVector, Array[Rect]] = _.get

final case class FaceClassifier private (
  classifier: CascadeClassifier = new CascadeClassifier(),
  classifiers: NonEmptyList[String] = NonEmptyList.fromListUnsafe(
    List(
      "/haarcascades/haarcascade_frontalface_default.xml",
      "/haarcascades/haarcascade_frontalface_alt.xml",
      "/haarcascades/haarcascade_frontalface_alt2.xml"
    )
  )
):
  private def resolveClassifier(name: String): IO[Path] =
    IO(getClass.getResource(name).getPath).map(Paths.get(_))

  private def loadClassifiers(): IO[NonEmptyList[Boolean]] =
    classifiers
      .traverse(resolveClassifier)
      .flatMap(_.traverse(p => IO(classifier.load(p))))

  def detectIn(inputImage: Mat): IO[Array[Rect]] = IO {
    val (width, height) = inputImage.cols() -> inputImage.rows()
    val grayImage       = new Mat(width, height, CV_8UC1)
    cvtColor(inputImage, grayImage, CV_BGR2GRAY)
    val faces           = new RectVector()
    classifier.detectMultiScale(grayImage, faces)
    faces
  }

object OCV:
  def readImage(path: Path): IO[Mat]                = IO(imread(path))
  def writeImage(path: Path, source: Mat): IO[Path] = IO(imwrite(path, source)).as(path)
  def drawRect(source: Mat, color: Scalar = new Scalar(0, 0, 255, 200), width: Int = 10)(rect: Rect): IO[Mat] = IO:
    val (x, y, w, h) = (rect.x, rect.y, rect.width, rect.height)
    rectangle(
      source,
      new Point(x, y),
      new Point(x + w, y + h),
      color,
      width,
      CV_AA,
      0
    )
    source

object FaceClassifier:

  def load(): IO[FaceClassifier] =
    IO(new FaceClassifier()).flatTap(_.loadClassifiers())

  val resource: Resource[IO, FaceClassifier] = load().toResource

object DetectOnImageApp
    extends CommandIOApp(
      name = "detect-on-image",
      header = "Face detection in static image"
    ):
  private val logger: SelfAwareStructuredLogger[IO] = LoggerFactory[IO].getLogger

  override def main: Opts[IO[ExitCode]] =
    val inputPathOps  = Opts.argument[Path]("input-image-path")
    val outputPathOps = Opts.argument[Path]("output-image-path")

    (inputPathOps, outputPathOps).mapN { case (inputPath, outputPath) =>
      for _ <- FaceClassifier.resource.use: classifier =>
          for
            _         <- logger.info(s"Reading image $inputPath")
            input     <- OCV.readImage(inputPath)
            detection <-
              classifier
                .detectIn(input)
                .flatTap(d =>
                  logger.info(
                    s"Faces detected ${d.length}"
                  )
                )
            _         <- detection.toList.traverse(OCV.drawRect(input))
            _         <-
              OCV
                .writeImage(outputPath, input)
                .flatTap(p => logger.info(s"Wrote image with faces to $p"))
          yield ()
      yield ExitCode.Success
    }
