package student.json.mapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;

import student.data.Student;

public class Mapper {
	private final Mappings mappings;

	private final String dateFormat;
	private final String jsonText;
	private final HttpServletRequest request;
	private final Map<String, Object> map;
	private final Student businessObject;
	private ErrorMessages errorsMessages;

	public Mapper(Mappings mappings, String dateFormat, String jsonText, HttpServletRequest request, Map<String, ?> map,
			Student businessObject, ErrorMessages errorsMessages) {
		this.mappings = mappings;
		this.dateFormat = dateFormat;
		this.jsonText = jsonText;
		this.request = request;
		@SuppressWarnings("unchecked")
		Map<String, Object> tMap = (Map<String, Object>) map;
		this.map = tMap;
		this.businessObject = businessObject;
		this.errorsMessages=errorsMessages;
	}

	public Boolean toBoolean(Mapping<Boolean> mapping, String text) {
		if ("".equals(text)) {
			errorsMessages.addErrors(mapping.boPropertyName + "is Required");
			return false;
		}
		return parseBoolean(text);
	}

	public static Boolean parseBoolean(String text) {
		return Boolean.valueOf("on".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text));
	}

	public String toText(Mapping<String> mapping, String text) {
		if ("".equals(text)) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.REQUIRED);
			return "";
		}
		return text;
	}

	public Integer toInteger(Mapping<Integer> mapping, String numberText) {
		if ("".equals(numberText)) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.REQUIRED);
			return 0;
		}
		try {
			return Integer.parseInt(numberText);
		} catch (NumberFormatException e) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.INVALID);
			return 0;
		}
	}

	public Date toPlainDate(Mapping<Date> mapping, String dateText) {
		if ("".equals(dateText)) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.INVALID);
		}

		try {
			System.out.println(this.dateFormat+"  "+dateText);
			return new SimpleDateFormat(this.dateFormat).parse(dateText);
		} catch (ParseException e) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.INVALID);
		}
		return null;
	}

	public BigDecimal toNumber(Mapping<BigDecimal> mapping, String numberText) {
		if ("".equals(numberText)) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.REQUIRED);

			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(numberText);
		} catch (NumberFormatException e) {
			errorsMessages.addErrors(mapping.boPropertyName + MessageName.INVALID);
			return BigDecimal.ZERO;

		}
	}

	protected String getJsonPropertyValue(String formPropertyName, MappingSource source) {
		/*
		 * if (source == MappingSource.JSON) { return
		 * toString(jsonText.get(formPropertyName)); } else
		 */ if (source == MappingSource.MAP) {
			return toString(map.get(formPropertyName));
		}
		return request.getParameter(formPropertyName);
	}

	protected void setJsonPropertyValue(String formPropertyName, Object value, MappingSource source) {
		/*
		 * if (source == MappingSource.JSON) { jsonText.set(formPropertyName,
		 * value); return; } else
		 */if (source == MappingSource.MAP) {
			map.put(formPropertyName, value);
			return;
		}
		request.setAttribute(formPropertyName, value);
	}

	private static String toString(Object value) {
		if (value == null) {
			return null;
		}
		if (String.class.isInstance(value)) {
			return (String) value;
		}
		return value.toString();
	}

	PropertyUtils propertyUtils = new PropertyUtils();

	public void mapToBo() {
		for (Mapping<?> mapping : mappings.values()) {
			if (mapping.direction == MappingDirection.TO_JSON_ONLY) {
				continue;
			}

			String textValue = getJsonPropertyValue(mapping.jsonPropertyName, mapping.source);

			if (mapping.skipBlanks && textValue.isEmpty()) {
				continue;
			}

			Object value = parseValue(mapping, textValue);
			try {
				// propertyUtils.setProperty(businessObject,
				// mapping.boPropertyName, value);
				Field f1 = Student.class.getDeclaredField(mapping.boPropertyName);
				f1.setAccessible(true);

				f1.set(businessObject, value);
			} catch (RuntimeException e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void mapToForm() {
		for (Mapping<?> mapping : mappings.values()) {
			if (mapping.direction == MappingDirection.TO_BO_ONLY) {
				continue;
			}
			setJsonPropertyValue(mapping.jsonPropertyName, formatValue(mapping, getBoPropertyValue(mapping.boPropertyName)),
					mapping.source);
		}
	}

	private Object getBoPropertyValue(String name) {
		Object value = null;
		try {
			value = PropertyUtils.getProperty(businessObject, name);
		} catch (NestedNullException e) {
			value = null;
		} catch (RuntimeException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private Object parseValue(Mapping<?> mapping, String textValue) {
		Class<?> type = mapping.type;
		Object value = null;

		if (BigDecimal.class.isAssignableFrom(type)) {
			value = toNumber((Mapping<BigDecimal>) mapping, textValue);
		} else if (Date.class.isAssignableFrom(type)) {
			value = toPlainDate((Mapping<Date>) mapping, textValue);
		} else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
			value = toInteger((Mapping<Integer>) mapping, textValue);
		} else if (String.class.isAssignableFrom(type)) {
			value = toText((Mapping<String>) mapping, textValue);
		} else if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
			value = toBoolean((Mapping<Boolean>) mapping, textValue);
		} else {
			throw new MappingException(//
					"Mapping to " + mapping.boPropertyName + "(" + type + ") is not supported.");
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private String formatValue(Mapping<?> mapping, Object value) {
		if (value == null) {
			return "";
		}
		Class<?> type = mapping.type;
		if (BigDecimal.class.isAssignableFrom(type)) {
			return value.toString();
		} else if (Date.class.isAssignableFrom(type)) {
			return new SimpleDateFormat(this.dateFormat).format((Date) value);
		} else if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
			return ((Integer) value).toString();
		} else if (String.class.isAssignableFrom(type)) {
			return value.toString();
		} else if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
			return ((Boolean) value).toString();
		} else {
			throw new MappingException(//
					"Mapping to " + mapping.boPropertyName + "(" + type + ") is not supported.");
		}
	}

}
