package me.alanfoster.camelry.codegen

import javax.xml.bind.JAXBContext
import me.alanfoster.camelry.codegen.model._
import me.alanfoster.camelry.codegen.model.BeanInfo
import me.alanfoster.camelry.codegen.model.Metadata
import scala.collection.{mutable, immutable, JavaConversions}

import com.sun.xml.bind.v2.runtime.JAXBContextImpl
import com.sun.xml.bind.v2.model.runtime._
import com.sun.xml.bind.v2.model.impl.RuntimeElementPropertyInfoImpl

import scala.collection.JavaConverters._
import com.sun.xml.bind.v2.model.core.{EnumConstant, ElementPropertyInfo, AttributePropertyInfo}
import java.lang.reflect.{WildcardType, ParameterizedType, Method, Type}

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util
import org.apache.camel.model.{ProcessorDefinition, ProcessDefinition}
import java.lang.Iterable
import scala.collection


/**
 * The trait Generator for creating DomElement interfaces from JAXB annotated classes.
 * The generatorName itself should perform any data transforms required before passing
 * to a concrete implementation of #generate
 */
// TODO This could do with a tidy up once there is a happy path straight through with camel code generation
trait Generator {
  private val logger: Logger = LoggerFactory.getLogger(getClass)


  /**
   * @param metadata Additional metadata relevant to package/class generation
   * @param jaxbPaths The packages containing the jaxb.index file
   *                  eg "foo.bar.baz" implies there exists a file "foo/bar/baz/jaxb.index"
   * @return A tuple, in which the first value is the file name, and the second value is the class contents
   */
  def generateFiles(metadata: Metadata, jaxbPaths: List[String], classLoader: ClassLoader): List[(String, String)] = {
    val delimitedPaths: String = jaxbPaths.mkString(":")

    val context: JAXBContext = JAXBContext.newInstance(delimitedPaths, classLoader)

    val set: RuntimeTypeInfoSet = context.asInstanceOf[JAXBContextImpl].getTypeInfoSet
    val beans: mutable.Map[Class[_], _ <: RuntimeClassInfo] = set.beans().asScala
    val enums: mutable.Map[Class[_], _ <: RuntimeEnumLeafInfo] = set.enums().asScala

    val generatedBeans = generateBeans(metadata, beans)
    val generatedEnums = generateEnums(metadata, enums)

    (generatedBeans ::: generatedEnums).toList
  }

   def generateBeans(metadata : Metadata, beans: mutable.Map[Class[_], _ <: RuntimeClassInfo]): List[(String, String)] = {
     val files = beans
       //.filter({ case (key, value) => key.getSimpleName == "OnCompletionDefinition"})
       //.filter({ case (key, value) => key.getSimpleName == "ExpressionNode"})
       //.filter({ case (key, value) => key.getSimpleName == "PersonDatabase"})
       //.filter({ case (key, value) => key.getSimpleName == "AggregateDefinition"})
       //.filter({ case (key, value) => key.getSimpleName == "PersonDatabase"})
       //.filter({ case (key, value) => key.getSimpleName == "CatchDefinition"})
       //.filter({ case (key, value) => value.isElement && "aggregate".equals(value.getElementName.getLocalPart) })
       .map({
       case (clazz, clazzInfo) => (clazz.getSimpleName, generateBeanFile(metadata, clazz, clazzInfo))
     })
     files.toList
   }


  def generateEnums(metadata : Metadata, enums: mutable.Map[Class[_], _ <: RuntimeEnumLeafInfo]): List[(String, String)] = {
    enums
      .map({ case (clazz, enum) => (clazz.getSimpleName, generateEnum(metadata, enum))})
      .toList
  }

  def generateEnum(metadata: Metadata, enum: RuntimeEnumLeafInfo) = {
    val generatedEnum = generateEnumFile(metadata, new EnumInfo(
      simpleName = enum.getClazz.getSimpleName,
      baseType = getDataType(enum.getBaseType.getType),
      enumPairs =  enum.getConstants.asScala.map(enum => EnumPair(enum.getName, enum.getLexicalValue)).toList)
    ).trim
    logger.info("Generated enum :: " + generatedEnum)
    generatedEnum
  }

  def generateBeanFile(metadata: Metadata, clazz: Class[_], classInfo: RuntimeClassInfo): String = {
    logger.info("Generating file for {}", clazz)

    // Data transformation
    val propertyMap = PropertyMapper.groupProperties(classInfo)

    if(propertyMap("values").size > 1) {
      throw new IllegalArgumentException("Values list should be zero or one :: " + propertyMap("values").mkString)
    }

    val generatedText = generateBeanFile(
        metadata = metadata,
        beanInfo = new BeanInfo(
          simpleName = clazz.getSimpleName,
          xmlName =
              if(classInfo.getElementName == null) "AbstractClass"
              else classInfo.getElementName.getLocalPart,
          baseClass = Option(classInfo.getBaseClass),
          attributes = propertyMap("attributes").asInstanceOf[mutable.Buffer[AttributePropertyInfo[Type, Class[_]]]],
          // Note, elements is the concatenation of both XmlElements and XmlElementRef - we make no distinction between the two
          elements =
            getElements(propertyMap("elements").asInstanceOf[mutable.Buffer[ElementPropertyInfo[Type, Class[_]]]])
            ++ getElementRefs(propertyMap("elementRefs").asInstanceOf[mutable.Buffer[RuntimeReferencePropertyInfo]])
          ,
          value =
            if(propertyMap("values").size == 1) propertyMap("values").asInstanceOf[mutable.Buffer[RuntimeValuePropertyInfo]].head
            else null
        )
      )

    val result = generatedText.trim
    logger.info("Generated file :: \n" + result)

    result
  }

  def getElements(elements: mutable.Buffer[ElementPropertyInfo[Type, Class[_]]]): List[Element] = {
    elements
    .map(element => {
      new Element(
        name = element.getName.capitalize,
        isCollection = element.isCollection,
        references =
          element
            .getTypes
            .asScala
            .map(elementType => new Base(elementType.getTagName.getLocalPart, getDataType(elementType.getTarget.getType), "TODO"))
            .toSet
        ,
        dataType = {
          // XXX Force a call to getRawType, which isn't exposed via the JAXB API for some reason
          val declaredMethod: Method = element.getClass.getDeclaredMethod("getRawType")
          declaredMethod.setAccessible(true)
          val rawType: Type = declaredMethod.invoke(element).asInstanceOf[Type]
          getDataType(rawType)
        }
      )
    })
    .toList
  }

  def getElementRefs(elementRefs: mutable.Buffer[RuntimeReferencePropertyInfo]): List[Element] = {
    elementRefs
      .map(getElement)
      .toList
  }

  def getElement(elementRef: RuntimeReferencePropertyInfo): Element = {
    new Element (
      name = elementRef.getName.capitalize,
      isCollection = elementRef.isCollection,
      references = JavaConversions
        .asScalaSet(elementRef.getElements)
        .map(elementRef =>  new Base(name = elementRef.getElementName.getLocalPart, dataType = getDataType(elementRef.getType), rawDataType = "rawr"))
        .toSet,
      // Grab the IndividualType ourselves, and wrap later if required
      dataType = getDataType(elementRef.getRawType),
      rawDataType = getDataType(elementRef.getIndividualType),
      isRef = true
    )
  }

  // TODO Perhaps we can't assume a raw type will always refer to the newly generated classes?
  def getDataType(parentType: Type): String = parentType match {
    // Create the full java type declaration, ie 'List<Foo, Bar, Baz>'
    case individualType: ParameterizedType => {
      def createString(paramType: ParameterizedType) = {
        paramType.getRawType.asInstanceOf[Class[_]].getSimpleName +
          paramType.getActualTypeArguments.map(getDataType).mkString("<", ", ", ">")
      }

      // TODO Remove Camel Specific code
      if(individualType.getRawType.asInstanceOf[Class[_]].isAssignableFrom(classOf[ProcessorDefinition[Any]])) "ProcessDefinition"
      else createString(individualType)
    }
    case individualType: WildcardType => "?"
    case individualType: Class[_] => individualType.getSimpleName
  }

  def generateBeanFile(metadata: Metadata, beanInfo: BeanInfo): String
  def generateEnumFile(metadata: Metadata, enumInfo: EnumInfo): String
}


