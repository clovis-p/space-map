package lol.clovis.spacemap.service;

import lol.clovis.spacemap.model.GroupInfo;
import lol.clovis.spacemap.model.SpacecraftData;

import java.util.List;
import java.util.Map;

public interface SpacecraftSource {
    Map<String, GroupInfo> groups();
    List<SpacecraftData> fetchGroup(String groupId);
}
