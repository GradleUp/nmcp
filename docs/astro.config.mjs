// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
	site: 'https://gradleup.com',
	base: '/nmcp',
	integrations: [
		starlight({
			title: 'Nmcp',
			editLink: {
				baseUrl: 'https://github.com/GradleUp/nmcp/edit/main/docs/',
			},
			logo: {
				src: './src/assets/logo.svg'
			},
			social: {
				github: 'https://github.com/GradleUp/nmcp',
			},
			sidebar: [
				{ label: 'Quickstart', link: '/', },
        { label: 'Manual configuration', link: '/manual-configuration' },
        { label: 'Debugging', link: '/debugging' },
        { label: 'Development', link: '/development' },
				{ label: 'Maven Central FAQ', link: '/portal-faq' },
        { label: 'Programmatic API', link: '/programmatic-api' },
        { label: 'KDoc↗', link: 'https://gradleup.com/nmcp/kdoc/nmcp/index.html' },
        { label: 'GitHub↗', link: 'https://github.com/gradleup/nmcp' },
			],
		}),
	],
});
