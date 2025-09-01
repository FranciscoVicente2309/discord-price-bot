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

    /**
     * Construtor para injeção de dependência.
     * O Spring automaticamente fornecerá o Bean JDA que criamos.
     */
    public DiscordNotificationService(JDA jda) {
        this.jda = jda;
    }

    /**
     * Envia um alerta de promoção para um canal de texto público configurado.
     */
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

    /**
     * Envia um alerta de promoção por mensagem direta (DM) para um usuário específico.
     * Só tenta enviar se o userId estiver configurado no application.properties.
     */
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

    /**
     * Método auxiliar privado para criar o texto da mensagem.
     * Evita duplicação de código e formata a mensagem de forma inteligente.
     */
    private String createPromotionMessage(BigDecimal newPrice, BigDecimal oldPrice, String productUrl) {
        // Se o preço antigo for nulo (primeira execução), exibe "N/A".
        // Caso contrário, formata o preço com duas casas decimais.
        String oldPriceText = (oldPrice == null) ? "N/A" : String.format("R$ %.2f", oldPrice);

        return String.format(
                """
                        @everyone\s
                🎉 **ALERTA DE PROMOÇÃO!** 🎉
                
                O preço do produto baixou!
                
                **Preço Anterior:** %s
                **Novo Preço:** **R$ %.2f**
                
                Corre pra ver! ➡️ %s
                """, oldPriceText, newPrice, productUrl
        );
    }

    /**
     * Método auxiliar para tratar erros comuns ao enviar DMs.
     */
    private void handleDmFailure(Throwable failure, User user) {
        if (failure instanceof ErrorResponseException e && e.getErrorResponse() == ErrorResponse.CANNOT_SEND_TO_USER) {
            System.err.println("Falha ao enviar DM: O usuário " + user.getName() + " provavelmente tem DMs bloqueadas.");
        } else {
            System.err.println("Falha ao enviar DM para " + user.getName() + ": " + failure.getMessage());
        }
    }
}