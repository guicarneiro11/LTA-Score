import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';

export const monitorMatchStateChanges = functions.firestore
    .document('matches/{matchId}')
    .onUpdate(async (change, context) => {
        const estadoAnterior = change.before.data().state;
        const novoEstado = change.after.data().state;
        const matchId = context.params.matchId;

        console.log(`[DEBUG] Partida ${matchId}: Estado anterior=${estadoAnterior}, Novo estado=${novoEstado}`);

        if (estadoAnterior === novoEstado) {
            console.log(`[DEBUG] Estados iguais, ignorando`);
            return null;
        }

        const dadosDaPartida = change.after.data();
        const time1 = dadosDaPartida.teams[0];
        const time2 = dadosDaPartida.teams[1];

        if (estadoAnterior === "UNSTARTED" && novoEstado === "INPROGRESS") {
            console.log(`[DEBUG] Transição de UNSTARTED para INPROGRESS detectada`);

            const titulo = `${time1.code} vs ${time2.code} está começando!`;
            const corpo = `A partida entre ${time1.name} e ${time2.name} está ao vivo!`;
            await enviarNotificacoesParaUsuariosInscritos(matchId, titulo, corpo, 'liveMatchNotifications');
        }

        else if (estadoAnterior === "INPROGRESS" && novoEstado === "COMPLETED") {
            console.log(`[DEBUG] Transição de INPROGRESS para COMPLETED detectada`);

            let codigoVencedor, codigoPerdedor, placar;
            if (time1.result && time1.result.outcome === 'WIN') {
                codigoVencedor = time1.code;
                codigoPerdedor = time2.code;
                placar = `${time1.result.gameWins}-${time2.result.gameWins}`;
            } else {
                codigoVencedor = time2.code;
                codigoPerdedor = time1.code;
                placar = `${time2.result.gameWins}-${time1.result.gameWins}`;
            }

            const titulo = `${codigoVencedor} venceu ${codigoPerdedor}`;
            const corpo = `${codigoVencedor} ganhou por ${placar} contra ${codigoPerdedor}!`;
            await enviarNotificacoesParaUsuariosInscritos(matchId, titulo, corpo, 'resultNotifications');
        }
        else {
            console.log(`[DEBUG] Transição não reconhecida: ${estadoAnterior} -> ${novoEstado}`);
        }

        return null;
    });

async function enviarNotificacoesParaUsuariosInscritos(matchId: string, titulo: string, corpo: string, tipoDePreferencia: string) {
    try {
        console.log(`[DEBUG] Buscando usuários inscritos para ${tipoDePreferencia}`);

        const snapshot = await admin.firestore().collection('user_tokens')
            .where(tipoDePreferencia, '==', true)
            .get();

        if (snapshot.empty) {
            console.log('Nenhum usuário inscrito para este tipo de notificação');
            return;
        }

        // Coletar os tokens
        const tokens: string[] = [];
        snapshot.forEach(doc => {
            const token = doc.data().token;
            if (token) {
                tokens.push(token);
            }
        });

        console.log(`Encontrados ${tokens.length} tokens para notificação`);

        if (tokens.length === 0) {
            console.log('Nenhum token válido encontrado');
            return;
        }

        // Enviar individualmente para cada token
        console.log(`Enviando para ${tokens.length} tokens`);
        const sucessos: string[] = [];
        const falhas: string[] = [];

        // Usando apenas o método send() que é mais amplamente disponível
        for (const token of tokens) {
            try {
                const mensagem = {
                    notification: {
                        title: titulo,
                        body: corpo,
                    },
                    data: {
                        matchId: matchId,
                        click_action: 'FLUTTER_NOTIFICATION_CLICK',
                    },
                    token: token
                };

                await admin.messaging().send(mensagem);
                sucessos.push(token);
                console.log(`Notificação enviada com sucesso para ${token.substring(0, 10)}...`);
            } catch (error) {
                const errorMessage = error instanceof Error ? error.message : String(error);
                console.error(`Erro ao enviar para token ${token.substring(0, 10)}...: ${errorMessage}`);
                falhas.push(token);
            }
        }

        console.log(`${sucessos.length} mensagens enviadas com sucesso, ${falhas.length} falhas`);
    } catch (error) {
        const errorMessage = error instanceof Error ? error.message : String(error);
        console.error(`Erro ao enviar notificações: ${errorMessage}`);
    }
}