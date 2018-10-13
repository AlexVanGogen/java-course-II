package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_REFS_PATH;

public class RefList {
    @NotNull private Map<String, List<String>> refs;

    public RefList() throws IOException {
        final List<Object> list = new JSONArray(Files.lines(GIT_REFS_PATH).collect(Collectors.joining(","))).toList();
        refs = list
                .stream()
                .map(o -> new JSONObject((HashMap) o))
                .map(Ref::new)
                .collect(Collectors.groupingBy(Ref::getRevision, Collectors.mapping(Ref::getName, Collectors.toList())));
    }

    public @Nullable String getRevisionForRef(@NotNull String refName) {
        final Map.Entry<String, List<String>> refRecord = refs.entrySet()
                .stream()
                .filter(e -> e.getValue().contains(refName))
                .findFirst()
                .orElse(null);
        return refRecord == null ? null : refRecord.getKey();
    }

    public @NotNull List<String> getRefsToRevision(@NotNull String revision) {
        return refs.getOrDefault(revision, Collections.emptyList());
    }

    public void add(@NotNull Ref ref) {
        if (!refs.containsKey(ref.getRevision())) {
            refs.put(ref.getRevision(), Collections.emptyList());
        }
        refs.get(ref.getRevision()).add(ref.getName());
    }

    public void update(@NotNull String refName, @NotNull String revision) {
        remove(refName);
        add(new Ref(refName, revision));
    }

    public void remove(@NotNull String refName) {
        refs.forEach((name, refsList) -> refsList.remove(refName));
    }

    public void write() throws IOException {
        JSONArray refsJson = new JSONArray();
        refs.forEach((name, refsList) -> {
            refsList.forEach(ref -> {
                JSONObject refJson = new JSONObject();
                refJson.put("name", name);
                refJson.put("revision", ref);
                refsJson.put(refJson);
            });
        });
        Files.write(GIT_REFS_PATH, Collections.singletonList(refsJson.toString()));
    }
}
