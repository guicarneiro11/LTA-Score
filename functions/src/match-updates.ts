import * as functions from 'firebase-functions/v1';
import * as admin from 'firebase-admin';

export const monitorMatchStateChanges = functions.firestore
    .document('matches/{matchId}')
    .onUpdate(async (change, context) => {
        const estadoAnterior = change.before.data().state;
        const novoEstado = change.after.data().state;
        const matchId = context.params.matchId;

        if (estadoAnterior === novoEstado) {
            return null;
        }

        const dadosDaPartida = change.after.data();
        const time1 = dadosDaPartida.teams[0];
        const time2 = dadosDaPartida.teams[1];

        if (estadoAnterior === 'UNSTARTED' && novoEstado === 'INPROGRESS') {
            const titulo = `${time1.code} vs ${time2.code} está começando!`;
            const corpo = `A partida entre ${time1.name} e ${time2.name} está ao vivo!`;
            await enviarNotificacoesParaUsuariosInscritos(matchId, titulo, corpo, 'liveMatchNotifications');
        }

        else if (estadoAnterior === 'INPROGRESS' && novoEstado === 'COMPLETED') {
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

        return null;
    });

async function enviarNotificacoesParaUsuariosInscritos(matchId: string, titulo: string, corpo: string, tipoDePreferencia: string) {
    try {
        const snapshot = await admin.firestore().collection('user_tokens')
            .where(tipoDePreferencia, '==', true)
            .get();

        if (snapshot.empty) {
            console.log('Nenhum usuário inscrito para este tipo de notificação');
            return;
        }

        const tokens: string[] = [];
        snapshot.forEach(doc => {
            const token = doc.data().token;
            if (token) {
                tokens.push(token);
            }
        });

        if (tokens.length === 0) {
            console.log('Nenhum token válido encontrado');
            return;
        }

        const mensagem = {
            notification: {
                title: titulo,
                body: corpo,
            },
            data: {
                matchId: matchId,
                click_action: 'FLUTTER_NOTIFICATION_CLICK',
            },
            tokens: tokens,
        };

        const resposta = await admin.messaging().sendMulticast(mensagem);
        console.log(`${resposta.successCount} mensagens foram enviadas com sucesso`);

        if (resposta.failureCount > 0) {
            const tokensComFalha: string[] = [];
            resposta.responses.forEach((resp, idx) => {
                if (!resp.success) {
                    tokensComFalha.push(tokens[idx]);
                }
            });
            console.log('Lista de tokens que causaram falhas:', tokensComFalha);
        }
    } catch (erro) {
        console.error('Erro ao enviar notificações:', erro);
    }
}