package com.beesynch.app.rest.Service;


import com.beesynch.app.rest.Models.*;
import com.beesynch.app.rest.DTO.HiveDTO;
import com.beesynch.app.rest.Repo.HiveMembersRepo;
import com.beesynch.app.rest.Repo.HiveRepo;
import com.beesynch.app.rest.Repo.RankingRepo;
import com.beesynch.app.rest.Repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.Optional;

@Service
@Transactional
public class HiveService {

    @Autowired
    RankingRepo rankRepo;

    @Autowired
    HiveRepo hiveRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    HiveMembersRepo hiveMembersRepo;

    public Hive createHiveNAdmin(HiveDTO hiveDTO) {
        if (hiveRepo.existsByHiveName(hiveDTO.getHiveName())) {
            System.out.println("Hive " + hiveDTO.getHiveName() + " already exists");
            throw new RuntimeException("Hive " + hiveDTO.getHiveName() + " already exists");
        }

        // Step 1: Fetch user who is creating the hive
        Optional<User> optionalUser = userRepo.findById(hiveDTO.getUserid());

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get(); // ✅ Declare user here

        // Step 2: Create hive
        Hive hive = new Hive();
        hive.setHive_id(hiveDTO.getHiveId());
        hive.setHiveName(hiveDTO.getHiveName());
        hive.setHive_created_date(Date.valueOf(java.time.LocalDate.now()));
        hive.setImg_path(hiveDTO.getImg_path());
        hive.setCreatedBy(user); // ✅ Now 'user' is defined correctly

        // Step 3: Make user an admin if not already
        if (!user.getIsAdmin()) {
            userRepo.changeToAdmin(user.getId());
        }

        return hiveRepo.save(hive);
    }


    public Hive updateHive(HiveDTO hiveDTO) {
        //step 1 fetch hive by id
        Hive existingHive = hiveRepo.findById(hiveDTO.getHiveId())
                .orElseThrow(() -> new RuntimeException("Hive not found with ID " + hiveDTO.getHiveId()));

        existingHive.setHiveName(hiveDTO.getHiveName());
        if(hiveDTO.getImg_path() != null){
            existingHive.setImg_path(hiveDTO.getImg_path());
        }

        return hiveRepo.save(existingHive);

    }


    public void addMemberToHive(Long adminUserId, String memberUsername) {
        // Checks if adminUserId or memberUsername is null or empty.
        if (adminUserId == null || memberUsername == null || memberUsername.isBlank()) {
            throw new IllegalArgumentException("Admin user ID and member username must be provided.");
        }

        // Step 1: Find user
        User adminUser = userRepo.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminUserId));
        System.out.println("Found user: " + adminUser.getUser_name());


        // Step 2: Checks if the user is really an admin or a member
        Boolean isAdmin = userRepo.findIsAdminByUserId(adminUserId);
        if (Boolean.FALSE.equals(isAdmin)) {
            throw new RuntimeException("User with ID " + adminUserId + " is not an admin.");
        }


        // Step 3: Finds the Hive Managed by the Admin user
        Hive hive = hiveRepo.findByCreatedBy(adminUser)
                .orElseThrow(() -> new RuntimeException("No hive found for this admin."));
        System.out.println("Found hive: " + hive.getHiveName());


        // Step: 4 Finds the user to be Added through their username
        User memberUser = userRepo.findByUserName(memberUsername);
        if (memberUser == null) {
            throw new RuntimeException("User not found.");
        }
        System.out.println("Found member user: " + memberUser.getUser_name());


        // Step 5: Checks if the found user is already a member of the admin user's hive
        boolean isMember = hiveMembersRepo.existsByUserIdAndHiveId(memberUser.getId(), hive.getHive_id());
        if (isMember) {
            throw new RuntimeException("User is already a member of this hive.");
        }


        // Step 6: Creates the new member's entry and then saved to the database
        // Ranking
        Ranking rank = new Ranking();
        rank.setUser_id(memberUser);
        rank.setHive_id(hive);
        rank.setRank_position(0);
        rank.setPeriod_start(new java.sql.Date(System.currentTimeMillis()));
        rank.setPeriod_end(null);
        rankRepo.save(rank);

        // Hive Member
        HiveMembers newMember = new HiveMembers();
        HiveMemberId id = new HiveMemberId(memberUser.getId(), hive.getHive_id());
        newMember.setHive_id(hive);
        newMember.setUser_id(memberUser);
        newMember.setId(id);
        newMember.setRanking_id(rank);
        newMember.setRole("Member");
        newMember.setAchievements(null);
        newMember.setPoints(0);
        hiveMembersRepo.save(newMember);

        System.out.println("User added successfully.");
    }


}