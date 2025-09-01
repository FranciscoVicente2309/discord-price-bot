package com.precificatron.discord_price_bot.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DiscordNotificationService {

    private final JDA jda;

    @Value("${discord.channel.id}")
    private long channelId;

    // A propriedade userId é opcional. Se não for encontrada, o valor padrão será -1.
    @Value("${discord.user.id:-1}")
    private long userId;


    public DiscordNotificationService(JDA jda) {
        this.jda = jda;
    }


    public void sendPublicPromotionAlert(BigDecimal newPrice, BigDecimal oldPrice, String productUrl) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String message = createPromotionMessage(newPrice, oldPrice, productUrl);
            channel.sendMessage(message).queue(); // Enfileira a mensagem para ser enviada
            System.out.println("Notificação de promoção enviada para o canal #" + channel.getName());
        } else {
            System.err.println("ERRO: Canal do Discord com ID " + channelId + " não foi encontrado.");
        }
    }


    public void sendPrivatePromotionAlert(BigDecimal newPrice, BigDecimal oldPrice, String productUrl) {
        if (userId == -1) {
            System.out.println("ID de usuário para DM não configurado. Pulando notificação privada.");
            return; // Interrompe a execução se não houver ID de usuário
        }

        jda.retrieveUserById(userId).queue(user -> {
            user.openPrivateChannel().queue(privateChannel -> {
                String message = createPromotionMessage(newPrice, oldPrice, productUrl);
                privateChannel.sendMessage(message).queue(
                        success -> System.out.println("DM de promoção enviada com sucesso para " + user.getName()),
                        failure -> handleDmFailure(failure, user)
                );
            });
        }, failure -> System.err.println("ERRO: Não foi possível encontrar o usuário com ID " + userId));
    }


    private String createPromotionMessage(BigDecimal newPrice, BigDecimal oldPrice, String productUrl) {

        String oldPriceText = (oldPrice == null) ? "N/A" : String.format("R$ %.2f", oldPrice);

        return String.format(
                """
                        
                🎉 **ALERTA DE PROMOÇÃO!** 🎉
                
                O preço do produto baixou @everyone\s!
                
                **Preço Anterior:** %s
                **Novo Preço:** **R$ %.2f**
                
                Corre pra ver! ➡️ %s
                """, oldPriceText, newPrice, productUrl
        );
    }


    private void handleDmFailure(Throwable failure, User user) {
        if (failure instanceof ErrorResponseException e && e.getErrorResponse() == ErrorResponse.CANNOT_SEND_TO_USER) {
            System.err.println("Falha ao enviar DM: O usuário " + user.getName() + " provavelmente tem DMs bloqueadas.");
        } else {
            System.err.println("Falha ao enviar DM para " + user.getName() + ": " + failure.getMessage());
        }
    }
}