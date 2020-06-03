package project.models.enums;

public enum GlobalSettingsEnum {
    MULTIUSER_MODE("Многопользовательский режим"),
    POST_PREMODERATION("Премодерация постов"),
    STATISTICS_IS_PUBLIC("Показывать всем статистику блога");

    private final String codeName;

    GlobalSettingsEnum(String code) {
        this.codeName = code;
    }

    public String getCodeName() {
        return codeName;
    }
}
