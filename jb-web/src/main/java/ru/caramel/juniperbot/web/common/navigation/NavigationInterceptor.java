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
package ru.caramel.juniperbot.web.common.navigation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class NavigationInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private MenuBuilder builder;

    @Autowired
    private ApplicationContext context;

    @Override
    public void postHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
            throws Exception {
        if (modelAndView == null) {
            return;
        }

        Set<MenuItem> toParse = new HashSet<>();

        List<MenuItem> menuItems = builder.build();
        toParse.addAll(menuItems);
        modelAndView.addObject("navigationMenu", menuItems);

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Navigation navigation = handlerMethod.getMethod().getAnnotation(Navigation.class);
            if (navigation == null) {
                navigation = handlerMethod.getBeanType().getAnnotation(Navigation.class);
            }

            if (navigation != null) {
                PageElement pageElement = navigation.value();

                MenuItem item = findForElement(pageElement, menuItems);
                if (item != null) {
                    item.setCurrent(true);

                    List<MenuItem> breadCrumb = new ArrayList<>();
                    while (item != null) {
                        breadCrumb.add(item);
                        item.setActive(true);
                        item = item.getParent();
                    }
                    if (modelAndView.getModel().containsKey("serverName")) {
                        breadCrumb.add(new MenuItem(PageElement.SERVERS));
                    }
                    breadCrumb.add(new MenuItem(PageElement.HOME));
                    Collections.reverse(breadCrumb);
                    toParse.addAll(breadCrumb);
                    modelAndView.addObject("breadCrumb", breadCrumb);
                }
            }
        }
        parseMenu(request, modelAndView, toParse);
    }

    private MenuItem findForElement(PageElement element, List<MenuItem> items) {
        for (MenuItem item : items) {
            if (element.equals(item.getElement())) {
                return item;
            }
            if (item.getChilds() != null) {
                MenuItem child = findForElement(element, item.getChilds());
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }

    private Collection<MenuItem> parseMenu(HttpServletRequest request, ModelAndView modelAndView, Collection<MenuItem> items) {
        Map<String, Object> sourceMap = modelAndView.getModel();
        final String[] search = new String[sourceMap.size()];
        final String[] replace = new String[sourceMap.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            search[i] = String.format("${%s}", entry.getKey());
            replace[i++] = String.valueOf(entry.getValue());
        }
        items = parseItem(request, search, replace, items);
        return items;
    }

    private Collection<MenuItem> parseItem(HttpServletRequest request, String[] search, String[] replace, Collection<MenuItem> items) {
        for (MenuItem item : items) {
            item.setUrl(StringUtils.replaceEach(item.getUrl(), search, replace));
            String message = context.getMessage(item.getName(), null, RequestContextUtils.getLocale(request));
            item.setName(StringUtils.replaceEach(message, search, replace));
            if (!item.getChilds().isEmpty()) {
                parseItem(request, search, replace, item.getChilds());
            }
        }
        return items;
    }
}