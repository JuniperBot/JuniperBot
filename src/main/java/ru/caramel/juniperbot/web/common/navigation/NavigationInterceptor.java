package ru.caramel.juniperbot.web.common.navigation;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class NavigationInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private MenuBuilder builder;

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

                MenuItem item = menuItems.stream().filter(e -> pageElement.equals(e.getElement())).findFirst().orElse(null);
                if (item != null) {
                    item.setCurrent(true);

                    List<MenuItem> breadCrumb = new ArrayList<>();
                    while (item != null) {
                        breadCrumb.add(item);
                        item.setActive(true);
                        item = item.getParent();
                    }
                    breadCrumb.add(new MenuItem(PageElement.SERVERS));
                    breadCrumb.add(new MenuItem(PageElement.HOME));
                    Collections.reverse(breadCrumb);
                    toParse.addAll(breadCrumb);
                    modelAndView.addObject("breadCrumb", breadCrumb);
                }
            }
        }
        parseMenu(modelAndView, toParse);
    }

    private Collection<MenuItem> parseMenu(ModelAndView modelAndView, Collection<MenuItem> items) {
        Map<String, Object> sourceMap = modelAndView.getModel();
        final String[] search = new String[sourceMap.size()];
        final String[] replace = new String[sourceMap.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            search[i] = String.format("${%s}", entry.getKey());
            replace[i++] = String.valueOf(entry.getValue());
        }
        items = parseItem(search, replace, items);
        return items;
    }

    private static Collection<MenuItem> parseItem(String[] search, String[] replace, Collection<MenuItem> items) {
        for (MenuItem item : items) {
            item.setUrl(StringUtils.replaceEach(item.getUrl(), search, replace));
            item.setName(StringUtils.replaceEach(item.getName(), search, replace));
            if (!item.getChilds().isEmpty()) {
                parseItem(search, replace, item.getChilds());
            }
        }
        return items;
    }
}