package mobi.f2time.dorado.swagger.ext;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.converter.ModelConverters;
import io.swagger.models.parameters.CookieParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import mobi.f2time.dorado.rest.annotation.CookieParam;
import mobi.f2time.dorado.rest.annotation.HeaderParam;
import mobi.f2time.dorado.rest.annotation.Path;
import mobi.f2time.dorado.rest.annotation.PathVariable;
import mobi.f2time.dorado.rest.annotation.RequestParam;
import mobi.f2time.dorado.rest.http.MultipartFile;
import mobi.f2time.dorado.rest.util.MethodDescriptor;
import mobi.f2time.dorado.rest.util.MethodDescriptor.MethodParameter;
import mobi.f2time.dorado.rest.util.TypeUtils;

public class DefaultParameterExtension implements SwaggerExtension {

	final ObjectMapper mapper = Json.mapper();

	/**
	 * Adds additional annotation processing support
	 * 
	 * @param parameters
	 * @param annotation
	 * @param type
	 * @param typesToSkip
	 */
	private void handleAdditionalAnnotation(List<Parameter> parameters, Annotation annotation, final Type type,
			Set<Type> typesToSkip) {
		// DO NOTHING
	}

	private Property createProperty(Type type) {
		return enforcePrimitive(ModelConverters.getInstance().readAsProperty(type), 0);
	}

	private Property enforcePrimitive(Property in, int level) {
		if (in instanceof RefProperty) {
			return new StringProperty();
		}
		if (in instanceof ArrayProperty) {
			if (level == 0) {
				final ArrayProperty array = (ArrayProperty) in;
				array.setItems(enforcePrimitive(array.getItems(), level + 1));
			} else {
				return new StringProperty();
			}
		}
		return in;
	}

	@Override
	public List<Parameter> extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip,
			Iterator<SwaggerExtension> chain, MethodDescriptor methodDescriptor) {
		if (shouldIgnoreType(type, typesToSkip)) {
			return new ArrayList<Parameter>();
		}

		Path methodPathAnno = methodDescriptor.getMethod().getAnnotation(Path.class);
		String operationPath = methodPathAnno != null ? methodPathAnno.value() : null;

		List<Parameter> parameters = new ArrayList<Parameter>();
		for (MethodParameter _parameter : methodDescriptor.getParameters()) {
			Parameter parameter = null;
			Class<?> annotationType = _parameter.getAnnotationType();
			if (annotationType == MultipartFile.class) {
				FormParameter fp = new FormParameter().type("file").name(_parameter.getName());
				parameter = fp;
			} else if (annotationType == RequestParam.class) {
				QueryParameter fp = new QueryParameter().name(_parameter.getName());
				Property schema = createProperty(type);
				if (schema != null) {
					fp.setProperty(schema);
				}
				parameter = fp;
			} else if (annotationType == PathVariable.class) {
				PathParameter fp = new PathParameter().name(_parameter.getName());
				Property schema = createProperty(type);
				if (schema != null) {
					fp.setProperty(schema);
				}
				parameter = fp;
			} else if (annotationType == HeaderParam.class) {
				HeaderParameter hp = new HeaderParameter().name(_parameter.getName());
				Property schema = createProperty(type);
				if (schema != null) {
					hp.setProperty(schema);
				}
				parameter = hp;
			} else if (annotationType == CookieParam.class) {
				CookieParameter cp = new CookieParameter().name(_parameter.getName());
				Property schema = createProperty(type);
				if (schema != null) {
					cp.setProperty(schema);
				}
				parameter = cp;
			} else {
				handleAdditionalAnnotation(parameters, _parameter.getAnnotation(), type, typesToSkip);
			}

			if (parameter == null) {
				if (operationPath != null && operationPath.contains(String.format("{%s}", _parameter.getName()))) {
					PathParameter fp = new PathParameter().name(_parameter.getName());
					Property schema = createProperty(type);
					if (schema != null) {
						fp.setProperty(schema);
					}
					parameter = fp;
				} else if (!isRequestBodyParam(_parameter)) {
					// 如果不是requestbody类型的参数，默认作为queryParameter
					QueryParameter fp = new QueryParameter().name(_parameter.getName());
					Property schema = createProperty(type);
					if (schema != null) {
						fp.setProperty(schema);
					}
					parameter = fp;
				}
			}

			if (parameter != null) {
				parameters.add(parameter);
			}
		}
		return parameters;
	}

	private boolean isRequestBodyParam(MethodParameter _parameter) {
		Class<?> type = _parameter.getType();

		if (byte[].class == type || InputStream.class == type) {
			return true;

		}
		if (TypeUtils.isSerializableType(type)) {
			return true;
		}

		return false;
	}
}
