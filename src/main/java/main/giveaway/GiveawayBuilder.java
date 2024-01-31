package main.giveaway;

import main.model.repository.ActiveGiveawayRepository;
import main.model.repository.ListUsersRepository;
import main.model.repository.ParticipantsRepository;

import java.sql.Timestamp;
import java.util.Map;

public interface GiveawayBuilder {

    class Builder {

        //Service
        private ActiveGiveawayRepository activeGiveawayRepository;
        private ParticipantsRepository participantsRepository;
        private ListUsersRepository listUsersRepository;
        private GiveawayMessageHandler giveawayMessageHandler;
        private GiveawaySaving giveawaySaving;
        private GiveawayEnd giveawayEnd;

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
        private Map<String, String> listUsersHash;

        public Builder setListUsersHash(Map<String, String> listUsersHash) {
            this.listUsersHash = listUsersHash;
            return this;
        }

        public Builder setGiveawayEnd(GiveawayEnd giveawayEnd) {
            this.giveawayEnd = giveawayEnd;
            return this;
        }

        public Builder setTime(String time) {
            this.time = time;
            return this;
        }

        public Builder setActiveGiveawayRepository(ActiveGiveawayRepository activeGiveawayRepository) {
            this.activeGiveawayRepository = activeGiveawayRepository;
            return this;
        }

        public Builder setListUsersRepository(ListUsersRepository listUsersRepository) {
            this.listUsersRepository = listUsersRepository;
            return this;
        }

        public Builder setParticipantsRepository(ParticipantsRepository participantsRepository) {
            this.participantsRepository = participantsRepository;
            return this;
        }

        public Builder setGiveawayMessageHandler(GiveawayMessageHandler giveawayMessageHandler) {
            this.giveawayMessageHandler = giveawayMessageHandler;
            return this;
        }

        public Builder setGiveawaySaving(GiveawaySaving giveawaySaving) {
            this.giveawaySaving = giveawaySaving;
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
            if (activeGiveawayRepository == null)
                throw new IllegalArgumentException("The provided activeGiveawayRepository cannot be null!");
            else if (participantsRepository == null)
                throw new IllegalArgumentException("The provided participantsRepository cannot be null!");
            else if (giveawaySaving == null)
                throw new IllegalArgumentException("The provided giveawaySaving cannot be null!");
            else if (listUsersRepository == null)
                throw new IllegalArgumentException("The provided listUsersRepository cannot be null!");

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
                    listUsersHash);
        }
    }
}
