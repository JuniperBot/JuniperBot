/*
 * This file is part of JuniperBotJ.
 *
 * JuniperBotJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * JuniperBotJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with JuniperBotJ. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.juniperbot.worker.common.shared.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.jasminb.jsonapi.DeserializationFeature;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JsonApiHttpMessageConverter implements GenericHttpMessageConverter<JSONAPIDocument> {

    private final ResourceConverter converter;

    public JsonApiHttpMessageConverter(Class<?>... classes) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.converter = new ResourceConverter(classes);
        this.converter.enableDeserializationOption(DeserializationFeature.ALLOW_UNKNOWN_INCLUSIONS);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return isAcceptable(type);
    }

    @Override
    public JSONAPIDocument read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        if (!isAcceptable(type)) {
            throw new IllegalArgumentException("Type not supported");
        }
        ParameterizedType rootType = getSubType(type);
        if (rootType != null && Collection.class.isAssignableFrom((Class) rootType.getRawType())) {
            Class<?> collectionType = getSubClass(rootType);
            if (collectionType != null) {
                return converter.readDocumentCollection(inputMessage.getBody(), collectionType);
            }
        }
        Class<?> rootClass = getSubClass(type);
        if (rootClass != null) {
            return converter.readDocument(inputMessage.getBody(), rootClass);
        }
        throw new IllegalArgumentException("No collection subtype found");
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public void write(JSONAPIDocument jsonapiDocument, Type type, MediaType contentType, HttpOutputMessage outputMessage) throws HttpMessageNotWritableException {

    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.emptyList();
    }

    @Override
    public JSONAPIDocument read(Class<? extends JSONAPIDocument> clazz, HttpInputMessage inputMessage) throws HttpMessageNotReadableException {
        return null;
    }

    @Override
    public void write(JSONAPIDocument jsonapiDocument, MediaType contentType, HttpOutputMessage outputMessage) throws HttpMessageNotWritableException {

    }

    private ParameterizedType getSubType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            var args = parameterizedType.getActualTypeArguments();

            if (args != null && args.length > 0 && args[0] instanceof ParameterizedType) {
                ParameterizedType subType = (ParameterizedType) args[0];
                if (Collection.class.isAssignableFrom((Class) subType.getRawType())) {
                    return subType;
                }
            }
        }
        return null;
    }

    private Class<?> getSubClass(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            var args = parameterizedType.getActualTypeArguments();

            if (args != null && args.length > 0 && args[0] instanceof Class) {
                return (Class) args[0];
            }
        }
        return null;
    }

    private boolean isAcceptable(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return JSONAPIDocument.class.isAssignableFrom((Class) parameterizedType.getRawType());
        }
        return false;
    }
}
