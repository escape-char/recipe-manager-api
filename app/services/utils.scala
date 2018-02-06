package services
import play.api.libs.json._
import play.api.data.format.Formatter
import play.api.data.{FormError, Forms, Mapping}


/**
  * Enumeration utility for read/write JSON operations on Enums
  */
object EnumUtils {
  def enumJsonReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    println("inside enum json reads")

    def reads(json: JsValue): JsResult[E#Value] = json match {
        case JsString(s) => {
          try {
            JsSuccess(enum.withName(s))
          } catch {
              case _: NoSuchElementException =>
                JsError(s"Enumeration expected of type: '${enum.getClass}',but it does not appear to contain the value: '$s'")
            }
          }
        case JsNull => {
            JsSuccess(null)
        }
        case _ => JsError("String or null value expected")
      }
  }
  implicit def enumJsonWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    println("inside enum json writes")
    def writes(v: E#Value): JsValue = if(v == null){JsNull}else{
      JsString(v.toString)
    }
  }

  implicit def enumJsonFormat[E <: Enumeration](enum: E): Format[E#Value] = {
      Format(enumJsonReads(enum), enumJsonWrites)
  }
  def enumForm[E <: Enumeration](enum: E): Mapping[E#Value] = Forms.of(enumFormFormat(enum))

  def enumFormFormat[E <: Enumeration](enum: E): Formatter[E#Value] = new Formatter[E#Value] {
    def bind(key: String, data: Map[String, String]) = {
      play.api.data.format.Formats.stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[E#Value]
        .either(enum.withName(s))
        .left.map(e => Seq(FormError(key, "error.enum", Nil)))
      }
    }
    def unbind(key: String, value: E#Value) = Map(key -> value.toString)
  }
}

/**
  * Basic utilities for form operations
  */
object Utils {
  def flattenFormErrors(errors:Seq[FormError]): Array[String]= {
    errors.toArray.map((e) => e.message)

  }
  def checkPasswordComplexity(p:String) : Boolean ={
    val numPattern= """[\d]""".r
    val alphaPattern = """[a-zA-Z]+""".r
    val symbolPattern = """[\!\@\#\$\%\^\&\*\(\)\_\+]""".r
    print("regex alpha pattern: " + alphaPattern.regex + "\n")
    print("password: " + p + "\n")
    print("num pattern: " + numPattern.findFirstIn(p).toString + "\n")
    print("alpha pattern: " + alphaPattern.findFirstIn(p).toString + "\n")
    print("symbol pattern: " + symbolPattern.findFirstIn(p).toString + "\n")
    print("\n")
    p match {
      case noNum if numPattern.findFirstIn(p) == None => false
      case noAlpha if alphaPattern.findFirstIn(p) == None => false
      case noSymbol if symbolPattern.findFirstIn(p) == None => false
      case _ => true
    }
  }
  def checkEmail(email:String): Boolean ={
    val emailPattern = "^[a-z0-9!#$%&'*+\\/=?^_`{|}~.-]+@[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$"
    email match{
      case emailValid if  email.matches(emailPattern) => true
      case _ => false
    }

  }
}
