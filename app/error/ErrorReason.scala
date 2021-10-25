package error

sealed trait ErrorReason { def msg: String }
case object InvalidJson extends ErrorReason { val msg = "Invalid json or missing header Content-Type:application/json" }
case object ServerError extends ErrorReason { val msg = "Server error. Try again later." }
case object SchemaNotFound extends ErrorReason { val msg = "Schema not found" }
case class JsonNotValidAgainstSchema(msg: String) extends ErrorReason

