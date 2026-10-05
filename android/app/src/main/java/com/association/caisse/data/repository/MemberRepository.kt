package com.association.caisse.data.repository

import com.association.caisse.data.local.dao.MemberDao
import com.association.caisse.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberRepository @Inject constructor(
    private val memberDao: MemberDao
) {
    fun getAllMembers(): Flow<List<MemberEntity>> = memberDao.getAllMembers()

    fun getActiveMembers(): Flow<List<MemberEntity>> = memberDao.getActiveMembers()

    suspend fun getMemberById(id: Long): MemberEntity? = memberDao.getMemberById(id)

    suspend fun addMember(member: MemberEntity): Long = memberDao.insertMember(member)

    suspend fun updateMember(member: MemberEntity) = memberDao.updateMember(member)

    suspend fun setMemberActive(id: Long, isActive: Boolean) = memberDao.setMemberActive(id, isActive)

    /**
     * Supprime le membre s'il ne possède aucun paiement ni recette.
     * @return true si suppression réussie, false si des paiements existent (invitation à archiver).
     */
    suspend fun deleteMemberSafely(member: MemberEntity): Boolean {
        val paymentsCount = memberDao.getPaymentsCountForMember(member.id)
        val otherIncomesCount = memberDao.getOtherIncomesCountForMember(member.id)
        if (paymentsCount > 0 || otherIncomesCount > 0) {
            return false
        }
        memberDao.deleteMember(member)
        return true
    }
}
