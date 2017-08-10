package ru.caramel.juniperbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.caramel.juniperbot.persistence.entity.User;
import ru.caramel.juniperbot.persistence.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public User createNew(String name1, String name2) {
        User user = new User();
        user.setName(name1);
        user.setName2(name2);
        return userRepository.save(user);
    }

    @Transactional
    public List<User> updateAll(List<User> users) {
        return userRepository.save(users);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
