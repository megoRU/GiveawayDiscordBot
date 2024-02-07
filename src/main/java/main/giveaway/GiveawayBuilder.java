package main.giveaway;

import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Set;

public interface GiveawayBuilder {

    @Service
    class Builder {

        private final ActiveGiveawayRepository activeGiveawayRepository;
        private final ParticipantsRepository participantsRepository;
        private final ListUsersRepository listUsersRepository;
        private final GiveawayMessageHandler giveawayMessageHandler;
        private final GiveawaySaving giveawaySaving;
        private final GiveawayEnd giveawayEnd;

        @Autowired
        public Builder(ActiveGiveawayRepository activeGiveawayRepository,
                       ParticipantsRepository participantsRepository,
                       ListUsersRepository listUsersRepository,
                       GiveawayMessageHandler giveawayMessageHandler,
                       GiveawaySaving giveawaySaving,
                       GiveawayEnd giveawayEnd) {
            this.activeGiveawayRepository = activeGiveawayRepository;
            this.participantsRepository = participantsRepository;
            this.listUsersRepository = listUsersRepository;
            this.giveawayMessageHandler = giveawayMessageHandler;
            this.giveawaySaving = giveawaySaving;
            this.giveawayEnd = giveawayEnd;
        }

        private long messageId;
        private int countWinners;
        private Long roleId;
        private boolean isForSpecificRole;
        private String urlImage;
        private String title;
        private String time;
        private Timestamp endGiveawayDate;
        private int minParticipants = 2;
        private long guildId;
        private long textChannelId;
        private long userIdLong;
        private boolean finishGiveaway;
        private Set<String> listUsersHash;
        private Long forbiddenRole;

        public Builder setForbiddenRole(Long forbiddenRole) {
            this.forbiddenRole = forbiddenRole;
            return this;
        }

        public Builder setListUsersHash(Set<String> listUsersHash) {
            this.listUsersHash = listUsersHash;
            return this;
        }

        public Builder setTime(String time) {
            this.time = time;
            return this;
        }

        public Builder setMessageId(long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder setCountWinners(int countWinners) {
            this.countWinners = countWinners;
            return this;
        }

        public Builder setRoleId(Long roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder setForSpecificRole(boolean forSpecificRole) {
            isForSpecificRole = forSpecificRole;
            return this;
        }

        public Builder setUrlImage(String urlImage) {
            this.urlImage = urlImage;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setEndGiveawayDate(Timestamp endGiveawayDate) {
            this.endGiveawayDate = endGiveawayDate;
            return this;
        }

        public Builder setMinParticipants(int minParticipants) {
            this.minParticipants = minParticipants;
            return this;
        }

        public Builder setGuildId(long guildId) {
            this.guildId = guildId;
            return this;
        }

        public Builder setTextChannelId(long textChannelId) {
            this.textChannelId = textChannelId;
            return this;
        }

        public Builder setUserIdLong(long userIdLong) {
            this.userIdLong = userIdLong;
            return this;
        }

        public Builder setFinishGiveaway(boolean finishGiveaway) {
            this.finishGiveaway = finishGiveaway;
            return this;
        }

        /**
         * @throws IllegalArgumentException activeGiveawayRepository == null и так далее
         */
        public Giveaway build() {
            Giveaway giveaway = new Giveaway(activeGiveawayRepository,
                    participantsRepository,
                    listUsersRepository,
                    giveawayMessageHandler,
                    giveawaySaving,
                    giveawayEnd);

            return giveaway.update(
                    guildId,
                    textChannelId,
                    userIdLong,
                    messageId,
                    title,
                    countWinners,
                    time,
                    roleId,
                    isForSpecificRole,
                    urlImage,
                    minParticipants,
                    finishGiveaway,
                    endGiveawayDate,
                    listUsersHash,
                    forbiddenRole);
        }
    }
}