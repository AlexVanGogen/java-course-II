package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class Ref {
    private @NotNull String name;
    private @NotNull String revision;

    public Ref(@NotNull String name, @NotNull String revision) {
        this.name = name;
        this.revision = revision;
    }

    public Ref(@NotNull JSONObject refObject) {
        this.name = refObject.getString("name");
        this.revision = refObject.getString("revision");
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getRevision() {
        return revision;
    }
}
