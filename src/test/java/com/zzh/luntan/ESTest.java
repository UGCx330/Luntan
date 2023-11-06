package com.zzh.luntan;

import com.zzh.luntan.mapper.PostMapper;
import com.zzh.luntan.mapper.elasticsearch.PostRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = LuntanApplication.class)
public class ESTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostMapper discussMapper;

    @Test
    public void test() {
        // postRepository.saveAll(discussMapper.selectPost(101, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(102, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(103, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(111, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(112, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(131, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(132, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(133, 0, 100,0));
        // postRepository.saveAll(discussMapper.selectPost(134, 0, 100,0));
    }
}
