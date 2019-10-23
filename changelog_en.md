# JuniperBot Changelog:

## Version 7.3
 - Command `юзер/user` now contains last online date;
 - **[web]** Message templates in panel mode now supports message outside of panel;
 - **[web]** Added new message template variables:
   - `{server.owner}`
   - `{member.level}`
   - `{member.voiceTime}`
   - `{member.cookies}`
 - **[Ranking]** Fixed "Ranking is not available for this member"
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
 - Command `бонус/bonus` now supports its disabling using minus sign: `!bonus -`
 - Some small fixes and improvements.

## Version 7.2
 - Fix broken SoundCloud playback;
 - Some small fixes and improvements.

## Version 7.1
 - **[web]** Welcome option "Restore member's roles on re-join" was splited into two independent options for roles and nicknames;
 - **[ranking]** New moderator command `уровень/level` can be used to change member's ranking level;
 - **[custom commands/bonus]** Reaction roles using message custom command;
