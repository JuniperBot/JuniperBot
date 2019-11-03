# JuniperBot Changelog:

## Version 7.4
 - **[Ranking]** Ranking system improvements:
   - **[Patreon]** Exp multiplier in range 50% - 500%
   - **[Patreon]** Voice experience gaining (with multiplier support too);
   - Command `ранг/rank` now uses image cards;
 - **[music]** Music improvements:
   - **[Patreon]** Playback progress bar;
 - Fixed `очистить/clear` didn't cleared system messages;
 - Fixed `юзер/user` didn't worked for bots;
 - Some small fixes and improvements.

## Version 7.3
 - **[music]**
   - SoundCloud fixes (again...);
   - Yandex.Music track, playlists and albums playback has been implemented;
   - Music search will now use Yandex.Music instead or YouTube;
 - Command `юзер/user` now contains last online date;
 - **[web]** Message templates in panel mode now supports message outside of panel;
 - **[web]** Alias custom command now ignores the prefix in its content;
 - **[web]** Added new message template variables:
   - `{server.owner}`
   - `{member.level}`
   - `{member.voiceTime}`
   - `{member.cookies}`
 - **[Ranking]** Fixed "Ranking is not available for this member";
 - Following commands are now supporting ID as well as mention:
   - `юзер/user`
   - `аватар/avatar`
   - `бан/ban`
   - `кик/kick`
   - `мьют/mute`
   - `пред/warn`
   - `преды/warns`
   - `снятьпред/remwarn`
   - `размьют/unmute`
   - `очистить/clear`
   - `цвет/color`
 - **[audit]** New audit action type "Messages has been cleared" which logs usage of command `очистить/clear` as well as keeps deleted content in audit channel;
 - Command `бонус/bonus` now supports its disabling using minus sign: `!bonus -`;
 - Updated status icons for `юзер/user` and platform icons for `steam` commands;
 - Duration texts for mute commands has been fixed;
 - Fixed `повтор/repeat` command;
 - Some small fixes and improvements.

## Version 7.2
 - Fix broken SoundCloud playback;
 - Some small fixes and improvements.

## Version 7.1
 - **[web]** Welcome option "Restore member's roles on re-join" was splited into two independent options for roles and nicknames;
 - **[ranking]** New moderator command `уровень/level` can be used to change member's ranking level;
 - **[custom commands/bonus]** Reaction roles using message custom command;
