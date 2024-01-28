package com.pinkstack.facetrace

import cats.effect.IO

final case class Config(
  inputStream: String
)
object Config:
  val load: IO[Config] = IO.pure(
    Config(
      inputStream = "http://192.168.17.127:8000/stream.mjpg"
    )
  )
