package ru.caramel.juniperbot.modules.wiki.model;

import org.sweble.wikitext.engine.config.*;
import org.sweble.wikitext.engine.utils.DefaultConfig;

import java.util.ArrayList;
import java.util.Arrays;

public class WikiFurConfig extends
        DefaultConfig {
    public static WikiConfigImpl generate() {
        WikiConfigImpl c = new WikiConfigImpl();
        new WikiFurConfig().configureWiki(c);
        return c;
    }

    protected void configureSiteProperties(WikiConfigImpl c) {
        c.setSiteName("ВикиФур");
        c.setWikiUrl("http://ru.wikifur.com");
        c.setContentLang("ru");
        c.setIwPrefix("ru");
    }

    protected ParserConfigImpl configureParser(WikiConfigImpl c) {
        ParserConfigImpl pc = super.configureParser(c);

        // --[ Link classification and parsing ]--

        pc.setInternalLinkPrefixPattern(null);
        pc.setInternalLinkPostfixPattern("[a-z]+");

        return pc;
    }

    protected void addNamespaces(WikiConfigImpl c) {
        c.addNamespace(new NamespaceImpl(
                -2,
                "Media",
                "Media",
                false,
                false,
                Arrays.asList("Медиа")));

        c.addNamespace(new NamespaceImpl(
                -1,
                "Special",
                "Special",
                false,
                false,
                Arrays.asList("Служебная")));

        c.addNamespace(new NamespaceImpl(
                0,
                "",
                "",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                1,
                "Talk",
                "Talk",
                false,
                false,
                Arrays.asList("Обсуждение")));

        c.addNamespace(new NamespaceImpl(
                2,
                "User",
                "User",
                false,
                false,
                Arrays.asList("Участник")));

        c.addNamespace(new NamespaceImpl(
                3,
                "User talk",
                "User talk",
                false,
                false,
                Arrays.asList("Обсуждение участника")));

        c.addNamespace(new NamespaceImpl(
                4,
                "Wikipedia",
                "Project",
                false,
                false,
                Arrays.asList("Проект")));

        c.addNamespace(new NamespaceImpl(
                5,
                "Wikipedia talk",
                "Project talk",
                false,
                false,
                Arrays.asList("Обсуждение проекта")));

        c.addNamespace(new NamespaceImpl(
                6,
                "File",
                "File",
                false,
                true,
                Arrays.asList("Image", "Файл")));

        c.addNamespace(new NamespaceImpl(
                7,
                "File talk",
                "File talk",
                false,
                false,
                Arrays.asList("Image talk", "Обсуждение файла")));

        c.addNamespace(new NamespaceImpl(
                8,
                "MediaWiki",
                "MediaWiki",
                false,
                false,
                new ArrayList<String>()));

        c.addNamespace(new NamespaceImpl(
                9,
                "MediaWiki talk",
                "MediaWiki talk",
                false,
                false,
                Arrays.asList("Обсуждение MediaWiki")));

        c.addNamespace(new NamespaceImpl(
                10,
                "Template",
                "Template",
                false,
                false,
                Arrays.asList("Шаблон")));

        c.addNamespace(new NamespaceImpl(
                11,
                "Template talk",
                "Template talk",
                false,
                false,
                Arrays.asList("Обсуждение шаблона")));

        c.addNamespace(new NamespaceImpl(
                12,
                "Help",
                "Help",
                false,
                false,
                Arrays.asList("Справка")));

        c.addNamespace(new NamespaceImpl(
                13,
                "Help talk",
                "Help talk",
                false,
                false,
                Arrays.asList("Обсуждение справки")));

        c.addNamespace(new NamespaceImpl(
                14,
                "Category",
                "Category",
                false,
                false,
                Arrays.asList("Категория")));

        c.addNamespace(new NamespaceImpl(
                15,
                "Category talk",
                "Category talk",
                false,
                false,
                Arrays.asList("Обсуждение категории")));

        c.addNamespace(new NamespaceImpl(
                100,
                "Portal",
                "Portal",
                false,
                false,
                Arrays.asList("Портал")));

        c.addNamespace(new NamespaceImpl(
                101,
                "Portal talk",
                "Portal talk",
                false,
                false,
                Arrays.asList("Обсуждение портала")));

        c.addNamespace(new NamespaceImpl(
                108,
                "Book",
                "Book",
                false,
                false,
                Arrays.asList("Книга")));

        c.addNamespace(new NamespaceImpl(
                109,
                "Book talk",
                "Book talk",
                false,
                false,
                Arrays.asList("Обсуждение книги")));

        c.setDefaultNamespace(c.getNamespace(0));
        c.setTemplateNamespace(c.getNamespace(10));
    }

    protected void addInterwikis(WikiConfigImpl c) {
        c.addInterwiki(new InterwikiImpl(
                "ru",
                "http://ru.wikipedia.org/wiki/$1",
                true,
                false));

        c.addInterwiki(new InterwikiImpl(
                "en",
                "http://en.wikipedia.org/wiki/$1",
                true,
                false));
    }

    protected void addI18nAliases(WikiConfigImpl c) {
        c.addI18nAlias(new I18nAliasImpl(
                "expr",
                false,
                Arrays.asList("#expr:")));
        c.addI18nAlias(new I18nAliasImpl(
                "if",
                false,
                Arrays.asList("#if:")));
        c.addI18nAlias(new I18nAliasImpl(
                "ifeq",
                false,
                Arrays.asList("#ifeq:")));
        c.addI18nAlias(new I18nAliasImpl(
                "ifexpr",
                false,
                Arrays.asList("#ifexpr:")));
        c.addI18nAlias(new I18nAliasImpl(
                "iferror",
                false,
                Arrays.asList("#iferror:")));
        c.addI18nAlias(new I18nAliasImpl(
                "switch",
                false,
                Arrays.asList("#switch:")));
        c.addI18nAlias(new I18nAliasImpl(
                "ifexist",
                false,
                Arrays.asList("#ifexist:")));
        c.addI18nAlias(new I18nAliasImpl(
                "time",
                false,
                Arrays.asList("#time:")));
        c.addI18nAlias(new I18nAliasImpl(
                "titleparts",
                false,
                Arrays.asList("#titleparts:")));
        c.addI18nAlias(new I18nAliasImpl(
                "redirect",
                false,
                Arrays.asList("#REDIRECT")));
        c.addI18nAlias(new I18nAliasImpl(
                "currentmonth",
                true,
                Arrays.asList("CURRENTMONTH", "CURRENTMONTH2")));
        c.addI18nAlias(new I18nAliasImpl(
                "currentday",
                true,
                Arrays.asList("CURRENTDAY")));
        c.addI18nAlias(new I18nAliasImpl(
                "currentyear",
                true,
                Arrays.asList("CURRENTYEAR")));
        c.addI18nAlias(new I18nAliasImpl(
                "pagename",
                true,
                Arrays.asList("PAGENAME", "PAGENAME:")));
        c.addI18nAlias(new I18nAliasImpl(
                "pagenamee",
                true,
                Arrays.asList("PAGENAMEE", "PAGENAMEE:")));
        c.addI18nAlias(new I18nAliasImpl(
                "namespace",
                true,
                Arrays.asList("NAMESPACE", "NAMESPACE:")));
        c.addI18nAlias(new I18nAliasImpl(
                "talkspace",
                true,
                Arrays.asList("TALKSPACE")));
        c.addI18nAlias(new I18nAliasImpl(
                "subjectspace",
                true,
                Arrays.asList("SUBJECTSPACE", "ARTICLESPACE")));
        c.addI18nAlias(new I18nAliasImpl(
                "fullpagename",
                true,
                Arrays.asList("FULLPAGENAME")));
        c.addI18nAlias(new I18nAliasImpl(
                "fullpagenamee",
                true,
                Arrays.asList("FULLPAGENAMEE")));
        c.addI18nAlias(new I18nAliasImpl(
                "basepagename",
                true,
                Arrays.asList("BASEPAGENAME")));
        c.addI18nAlias(new I18nAliasImpl(
                "talkpagename",
                true,
                Arrays.asList("TALKPAGENAME", "TALKPAGENAME:")));
        c.addI18nAlias(new I18nAliasImpl(
                "subjectpagename",
                true,
                Arrays.asList("SUBJECTPAGENAME", "ARTICLEPAGENAME")));
        c.addI18nAlias(new I18nAliasImpl(
                "safesubst",
                false,
                Arrays.asList("SAFESUBST:")));
        c.addI18nAlias(new I18nAliasImpl(
                "sitename",
                true,
                Arrays.asList("SITENAME")));
        c.addI18nAlias(new I18nAliasImpl(
                "ns",
                false,
                Arrays.asList("NS:")));
        c.addI18nAlias(new I18nAliasImpl(
                "fullurl",
                false,
                Arrays.asList("FULLURL:")));
        c.addI18nAlias(new I18nAliasImpl(
                "lcfirst",
                false,
                Arrays.asList("LCFIRST:")));
        c.addI18nAlias(new I18nAliasImpl(
                "ucfirst",
                false,
                Arrays.asList("UCFIRST:")));
        c.addI18nAlias(new I18nAliasImpl(
                "lc",
                false,
                Arrays.asList("LC:")));
        c.addI18nAlias(new I18nAliasImpl(
                "uc",
                false,
                Arrays.asList("UC:")));
        c.addI18nAlias(new I18nAliasImpl(
                "urlencode",
                false,
                Arrays.asList("URLENCODE:")));
        c.addI18nAlias(new I18nAliasImpl(
                "contentlanguage",
                true,
                Arrays.asList("CONTENTLANGUAGE", "CONTENTLANG")));
        c.addI18nAlias(new I18nAliasImpl(
                "padleft",
                false,
                Arrays.asList("PADLEFT:")));
        c.addI18nAlias(new I18nAliasImpl(
                "defaultsort",
                true,
                Arrays.asList("DEFAULTSORT:", "DEFAULTSORTKEY:", "DEFAULTCATEGORYSORT:")));
        c.addI18nAlias(new I18nAliasImpl(
                "filepath",
                false,
                Arrays.asList("FILEPATH:")));
        c.addI18nAlias(new I18nAliasImpl(
                "tag",
                false,
                Arrays.asList("#tag:")));
        c.addI18nAlias(new I18nAliasImpl(
                "protectionlevel",
                true,
                Arrays.asList("PROTECTIONLEVEL:")));
        c.addI18nAlias(new I18nAliasImpl(
                "основная статья",
                false,
                Arrays.asList("основная статья|")));
        c.addI18nAlias(new I18nAliasImpl(
                "нет статьи",
                false,
                Arrays.asList("нет статьи|")));
    }
}
