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
package ru.juniperbot.common.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.juniperbot.common.persistence.entity.Playlist;
import ru.juniperbot.common.persistence.entity.PlaylistItem;
import ru.juniperbot.common.persistence.repository.PlaylistItemRepository;
import ru.juniperbot.common.persistence.repository.PlaylistRepository;
import ru.juniperbot.common.service.PlaylistService;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlaylistServiceImpl implements PlaylistService {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistItemRepository playlistItemRepository;

    @Override
    @Transactional
    public Playlist getPlaylist(String uuid) {
        return validateItems(playlistRepository.findByUuid(uuid));
    }

    @Override
    @Transactional
    public Playlist find(Long id) {
        return validateItems(playlistRepository.findById(id).orElse(null));
    }

    @Override
    @Transactional
    public Playlist save(Playlist playlist) {
        return playlistRepository.save(playlist);
    }

    @Override
    @Transactional
    public PlaylistItem save(PlaylistItem playlistItem) {
        return playlistItemRepository.save(playlistItem);
    }

    private Playlist validateItems(Playlist playlist) {
        if (playlist != null && CollectionUtils.isNotEmpty(playlist.getItems()) && playlist.getItems().contains(null)) {
            playlist.setItems(playlist.getItems().stream().filter(Objects::nonNull).collect(Collectors.toList()));
            playlistRepository.save(playlist);
        }
        return playlist;
    }
}
