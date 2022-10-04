package io.springboot.netty.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.springboot.netty.service.BizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "async")
public class AsyncContentConroller {

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 200, 50000L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(100));
    @Autowired
    private BizService bizService;

    @GetMapping(value = "/pull")
    public @ResponseBody void startServer(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("--------当前线程名称-------" + Thread.currentThread().getName());
        try {
            if (request.isAsyncSupported()) {
                //用于启动异步工作线程,进入异步模式,调用业务处理线程进行业务处理
                request.startAsync(request, response);
                log.info("start async task...");
                if (request.isAsyncStarted()) {
                    /**
                     * 1 获取AsyncContext，对异步执行的上下文提供支持，可以透过AsyncContext的getRequest() 、 getResponse()方法取得Request、Response对象
                     * 2  客户端的响应将暂缓至，调用AsyncContext的complete()方法或dispatch()为止，前者表示回应完成，后者表示将响应调派给指定的URL
                     * 3 使用异步处理方式，web容器的请求处理线程释放了，可以服务其他的请求处理。但是该Request的处理并没有结束，
                     *   在使用AsyncContext的complete或者dispatch完成后，这个request的处理才结束。
                     */
                    final AsyncContext asyncContext = request.getAsyncContext();
                    asyncContext.setTimeout(20000);
                    asyncContext.addListener(new AsyncListener() {
                        @Override
                        public void onComplete(AsyncEvent asyncEvent) throws IOException {
                            log.info("onComplete", asyncEvent.getAsyncContext().getTimeout());
                        }

                        @Override
                        public void onTimeout(AsyncEvent asyncEvent) throws IOException {
//                            request.startAsync(request, response);
//                            final AsyncContext asyncContext = request.getAsyncContext();
//                            asyncContext.setTimeout(5000);
                            log.info("onTimeout", asyncEvent.getAsyncContext().getTimeout());
                        }

                        @Override
                        public void onError(AsyncEvent asyncEvent) throws IOException {
                            log.info("onError:{}", asyncEvent.getThrowable());
                        }

                        @Override
                        public void onStartAsync(AsyncEvent asyncEvent) throws IOException {
                            log.info("onStartAsync:{}", asyncEvent.getAsyncContext().getTimeout());
                        }
                    });
                    System.out.println("--------超时时间------" + asyncContext.getTimeout());
                    // Servlet不会被阻塞,而是直接往下执行
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            PrintWriter printWriter = null;
                            try {
                                log.info("start to deal with ...");
                                Thread.sleep(40000);
                                response.setCharacterEncoding("UTF-8");
                                response.setHeader("Content-type", "application/json;charset=UTF-8");
                                printWriter = response.getWriter();
                                printWriter.write("jsonDto.toString()");
//                                request.startAsync(request, response);

                                final AsyncContext asyncContext = request.getAsyncContext();
                                //asyncContext.setTimeout(5000);
                                printWriter.write("jsonDto.toString    nnnnnnnnnnnnnnn");
                                log.info("push data to client..");
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (printWriter != null) {
                                    printWriter.flush();
                                    printWriter.close();
                                }
                                //告诉启动异步处理的Servlet异步处理已完成，Servlet就会提交请求响应
                                log.info("completed...");
                                asyncContext.complete();
                            }
                        }
                    });
                }
            } else { // 不支持异步
                System.out.println("当前servlet容器不支持异步....");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

